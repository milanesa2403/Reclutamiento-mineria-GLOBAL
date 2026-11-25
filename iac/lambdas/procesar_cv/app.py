import hashlib
import json
import os
import ssl

import boto3
import pg8000.native

s3 = boto3.client("s3")
secretos = boto3.client("secretsmanager")

CREAR_TABLA = """
CREATE TABLE IF NOT EXISTS cv_procesado (
    id BIGSERIAL PRIMARY KEY,
    documento_id TEXT,
    bucket TEXT NOT NULL,
    clave TEXT NOT NULL,
    sha256 CHAR(64),
    tamano_bytes BIGINT,
    tipo TEXT,
    estado TEXT NOT NULL,
    motivo TEXT,
    procesado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (clave, sha256)
)
"""

INSERTAR = """
INSERT INTO cv_procesado (documento_id, bucket, clave, sha256, tamano_bytes, tipo, estado, motivo)
VALUES (:documento, :bucket, :clave, :sha256, :tamano, :tipo, :estado, :motivo)
ON CONFLICT (clave, sha256) DO NOTHING
"""

TAMANO_MAXIMO = 5 * 1024 * 1024
_conexion = None


def conexion():
    global _conexion
    if _conexion is None:
        datos = json.loads(secretos.get_secret_value(SecretId=os.environ["DB_SECRET_ARN"])["SecretString"])
        contexto = ssl.create_default_context(cafile=os.path.join(os.path.dirname(__file__), "rds-ca.pem"))
        _conexion = pg8000.native.Connection(
            user=datos["username"],
            password=datos["password"],
            host=os.environ["DB_HOST"],
            database=os.environ["DB_NAME"],
            ssl_context=contexto,
            timeout=10,
        )
        _conexion.run(CREAR_TABLA)
    return _conexion


def revisar(bucket, clave):
    objeto = s3.get_object(Bucket=bucket, Key=clave)
    tamano = objeto["ContentLength"]
    tipo = objeto.get("ContentType")
    if tamano > TAMANO_MAXIMO:
        return None, tamano, tipo, "RECHAZADO", "Supera 5 MB"
    contenido = objeto["Body"].read()
    huella = hashlib.sha256(contenido).hexdigest()
    if not contenido.startswith(b"%PDF-"):
        return huella, tamano, tipo, "RECHAZADO", "No es un PDF"
    return huella, tamano, tipo, "VALIDO", None


def handler(event, context):
    global _conexion
    fallidos = []
    for mensaje in event["Records"]:
        try:
            cuerpo = json.loads(mensaje["body"])
            bucket = cuerpo.get("bucket", os.environ["CVS_BUCKET"])
            clave = cuerpo["clave"]
            huella, tamano, tipo, estado, motivo = revisar(bucket, clave)
            conexion().run(
                INSERTAR,
                documento=str(cuerpo.get("documentoId", "")) or None,
                bucket=bucket,
                clave=clave,
                sha256=huella,
                tamano=tamano,
                tipo=tipo,
                estado=estado,
                motivo=motivo,
            )
            print(json.dumps({"clave": clave, "sha256": huella, "estado": estado, "motivo": motivo}))
        except Exception as error:
            _conexion = None
            print(json.dumps({"error": str(error), "mensaje": mensaje["messageId"]}))
            fallidos.append({"itemIdentifier": mensaje["messageId"]})
    return {"batchItemFailures": fallidos}
