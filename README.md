# Proyecto de Infraestructura sobre Plataforma de Reclutamiento Minero

Proyecto: Plataforma global para reclutamiento de minería con infraestructura escalable.

Hemos desplegado con Terraform la infraestructura del proyecto en AWS usando GitHub Actions. La conexión con AWS es por OIDC y el estado de Terraform se guarda en un bucket S3.

## Ramas
- main: version final donde se encontrara todo el codigo
- development: aqui se une todo y se despliega en AWS
- releases/...: avances de cada integrante, se hace pull request a development

## Workflows
- github-actions-ReclutamientoMineria.yml: pipeline del proyecto
  - Validar-Infraestructura: fmt, init y validate en cada push y pull request (sin AWS)
  - Publicar-en-AWS: con push a development se conecta a AWS por OIDC y hace plan y apply
  - Comprobar-Publicacion: hace curl a la pagina creada
- destruir.yml: destroy manual desde Actions

Fuentes:
https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/trigger-a-workflow
https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/control-jobs-with-conditions
https://github.com/aws-actions/configure-aws-credentials
https://github.com/hashicorp/setup-terraform

## Terraform base
main.tf: version de Terraform, provider de AWS y backend S3 (el estado se guarda en lab09/dev/terraform.tfstate).
variables.tf: region principal y secundaria, entorno, prefijo, red, tamano del Auto Scaling, base de datos y correo de alertas.

Fuentes:
https://registry.terraform.io/providers/hashicorp/aws/latest/docs
https://developer.hashicorp.com/terraform/language/backend/s3
https://developer.hashicorp.com/terraform/language/values/variables

## Primer avance: infraestructura base
Sigue el diagrama de arquitectura en la region us-east-2. El backend y el frontend se agregan despues; por ahora cada EC2 corre un contenedor Nginx con una pagina de prueba y /api/health.

| Archivo | Componente del diagrama | Que crea |
|---|---|---|
| network.tf | VPC, internet gateway, subredes | VPC en las zonas A y B, una subred publica y una privada por zona, tablas de rutas, endpoint de S3 y flow logs |
| security.tf | Security groups | El ALB recibe HTTP, las EC2 solo aceptan trafico del ALB y RDS solo de las EC2 |
| loadbalancer.tf | 4. Application Load Balancer | ALB en las subredes publicas A y B, target group con health check en /api/health y logs de acceso en S3 |
| compute.tf, templates/user_data.sh.tftpl | 5. EC2 con Auto Scaling | Launch template con Amazon Linux 2023, IMDSv2 y disco cifrado; Auto Scaling de 1 a 3 instancias que agrega una si la CPU pasa de 70 %; Docker con Nginx y logs en CloudWatch |
| database.tf | 6. RDS principal | PostgreSQL 16 en la subred privada, cifrada, con respaldos automaticos de 7 dias, SSL obligatorio y contrasena en Secrets Manager |
| storage.tf | 10. Amazon S3 y copia diaria | Bucket de CVs con versionado y cifrado, replicado al bucket de la region secundaria (sa-east-1), y bucket de logs |
| monitoring.tf | 11. CloudWatch y SNS | Tema SNS con correo al equipo, alarmas (errores 5xx, latencia, instancias no sanas, CPU, conexiones y espacio de RDS) y dashboard |
| audit.tf | CloudTrail | Registro de auditoria con validacion de archivos, bucket propio y envio a CloudWatch |
| kms.tf | Cifrado | Llaves KMS con rotacion en la region principal y en la secundaria |

Quedan para los siguientes avances: CloudFront con ACM y HTTPS, Cognito, SQS con DLQ, Lambda y la RDS restaurada.

outputs.tf: muestra la URL del ALB (web_url), el Auto Scaling Group, el endpoint de RDS, los buckets, el tema SNS y el dashboard.

Para recibir las alarmas, pasar el correo con `-var="alert_email=correo@ejemplo.com"`.

Fuentes:
https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lb
https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/autoscaling_group
https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/db_instance
https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_replication_configuration
https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm
https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/user-data.html

## Configuracion AWS y GitHub
- Region us-east-2, MFA, alerta de presupuesto y bucket S3 para el estado
- Proveedor OIDC de GitHub y rol lab09-github-actions-dev (plantillas en iam/)
- iam/terraform-policy.json ahora incluye los servicios del avance (ELB, Auto Scaling, RDS, S3, SNS, KMS, CloudWatch, CloudTrail y los roles minero-*). Hay que reemplazar <TF_STATE_BUCKET> y <AWS_ACCOUNT_ID> y actualizar la politica del rol antes del primer apply
- Environment dev y variables AWS_REGION, AWS_ROLE_ARN y TF_STATE_BUCKET

Fuentes:
https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments/oidc-in-aws
https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_create_for-idp_oidc.html
https://docs.github.com/en/actions/how-tos/deploy/configure-and-manage-deployments/manage-environments
https://developer.hashicorp.com/terraform/language/values/outputs

## COMANDOS
Estos comandos se usan en la terminal de VS Code con Git Bash.

1.- Clonar el repositorio
```
git clone https://github.com/milanesa2403/Reclutamiento-mineria-GLOBAL.git
cd Reclutamiento-mineria-GLOBAL
```
2.- Crear una rama para subir un avance
```
git checkout development
git checkout -b releases/nombre-del-avance
git add .
git commit -m "mensaje"
git push -u origin releases/nombre-del-avance
```
Luego se hace el pull request a development en GitHub.

3.- Probar el codigo sin conectarse a AWS
```
terraform -chdir=terraform fmt
terraform -chdir=terraform init -backend=false
terraform -chdir=terraform validate
```
4.- Desplegar
Hacer merge a development. El pipeline valida, hace el apply en AWS y comprueba la pagina con curl.

5.- Eliminar todo
Actions > Destruir Infraestructura > Run workflow, rama development, escribir destroy.

Fuentes:
https://developer.hashicorp.com/terraform/cli/commands
---

## Estructura unificada

```
backend/   Spring Boot 3.3 (Java 17): login JWT + roles. H2 en memoria.
iac/       Terraform (iac/terraform) + politicas IAM (iac/iam).
.github/   CI: tests del backend + validar/aplicar/destruir Terraform.
```

### Backend local
```
cd backend
mvn verify            # compila y corre tests
mvn spring-boot:run   # http://localhost:8080
```
Admin de prueba (solo perfil distinto de `prod`): `admin@mineria.com` / `AdminMineria123!`.
En produccion define `JWT_SECRETO` (min. 32 bytes) y `SPRING_PROFILES_ACTIVE=prod`.

## Backend y Frontend
- `backend/`: API REST Spring Boot (Java 17). Ver `backend/README.md` (endpoints, mapa RNF -> codigo, como ejecutar y probar).
- `frontend/`: React + Vite (avance). Ver `frontend/README.md`.
- El pipeline ejecuta `mvn verify` (backend) y `npm run build` (frontend) en cada push y pull request.
