locals {
  colas = {
    cv = "${local.name}-procesar-cv"
  }
}

resource "aws_sqs_queue" "cv_dlq" {
  name                      = "${local.colas.cv}-dlq"
  kms_master_key_id         = aws_kms_key.main.arn
  message_retention_seconds = 1209600
}

resource "aws_sqs_queue" "cv" {
  name                       = local.colas.cv
  kms_master_key_id          = aws_kms_key.main.arn
  visibility_timeout_seconds = 180
  message_retention_seconds  = 345600

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.cv_dlq.arn
    maxReceiveCount     = 5
  })
}

resource "aws_vpc_endpoint" "secretsmanager" {
  vpc_id              = aws_vpc.main.id
  service_name        = "com.amazonaws.${data.aws_region.current.region}.secretsmanager"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = [aws_subnet.private[0].id]
  security_group_ids  = [aws_security_group.endpoints.id]
  private_dns_enabled = true

  tags = {
    Name = "${local.name}-endpoint-secretsmanager"
  }
}

output "cola_cv_url" {
  value = aws_sqs_queue.cv.url
}

data "archive_file" "procesar_cv" {
  type        = "zip"
  source_dir  = "${path.module}/../lambdas/procesar_cv"
  output_path = "${path.module}/.build/procesar_cv.zip"
}

resource "aws_cloudwatch_log_group" "lambda" {
  for_each          = toset(["procesar-cv"])
  name              = "/aws/lambda/${local.name}-${each.value}"
  retention_in_days = var.log_retention_days
  kms_key_id        = aws_kms_key.main.arn
}

resource "aws_iam_role" "procesar_cv" {
  name = "${local.name}-lambda-procesar-cv"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "lambda.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "procesar_cv_vpc" {
  role       = aws_iam_role.procesar_cv.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
}

resource "aws_iam_role_policy" "procesar_cv" {
  name = "permisos-procesar-cv"
  role = aws_iam_role.procesar_cv.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid      = "Registros"
        Effect   = "Allow"
        Action   = ["logs:CreateLogStream", "logs:PutLogEvents"]
        Resource = "${aws_cloudwatch_log_group.lambda["procesar-cv"].arn}:*"
      },
      {
        Sid      = "LeerCola"
        Effect   = "Allow"
        Action   = ["sqs:ReceiveMessage", "sqs:DeleteMessage", "sqs:GetQueueAttributes"]
        Resource = aws_sqs_queue.cv.arn
      },
      {
        Sid      = "LeerCvs"
        Effect   = "Allow"
        Action   = ["s3:GetObject", "s3:GetObjectVersion"]
        Resource = "${aws_s3_bucket.cvs.arn}/cvs/*"
      },
      {
        Sid      = "SecretoBaseDatos"
        Effect   = "Allow"
        Action   = "secretsmanager:GetSecretValue"
        Resource = local.db_activa.master_user_secret[0].secret_arn
      },
      {
        Sid      = "Cifrado"
        Effect   = "Allow"
        Action   = ["kms:Decrypt", "kms:GenerateDataKey"]
        Resource = aws_kms_key.main.arn
      },
      {
        Sid      = "Trazas"
        Effect   = "Allow"
        Action   = ["xray:PutTraceSegments", "xray:PutTelemetryRecords"]
        Resource = "*"
      }
    ]
  })
}

resource "aws_lambda_function" "procesar_cv" {
  #checkov:skip=CKV_AWS_115:Las cuentas nuevas solo tienen 10 ejecuciones simultaneas y AWS no deja reservar parte de ellas
  #checkov:skip=CKV_AWS_116:Los mensajes que fallan 5 veces ya pasan a la cola de errores de SQS
  #checkov:skip=CKV_AWS_272:El codigo lo sube Terraform desde el repositorio revisado por nosotros
  function_name    = "${local.name}-procesar-cv"
  role             = aws_iam_role.procesar_cv.arn
  runtime          = "python3.12"
  handler          = "app.handler"
  filename         = data.archive_file.procesar_cv.output_path
  source_code_hash = data.archive_file.procesar_cv.output_base64sha256
  timeout          = 30
  memory_size      = 256
  kms_key_arn      = aws_kms_key.main.arn

  vpc_config {
    subnet_ids         = aws_subnet.private[*].id
    security_group_ids = [aws_security_group.lambda.id]
  }

  tracing_config {
    mode = "Active"
  }

  environment {
    variables = {
      DB_HOST       = local.db_activa.address
      DB_NAME       = aws_db_instance.main.db_name
      CVS_BUCKET    = aws_s3_bucket.cvs.id
      DB_SECRET_ARN = local.db_activa.master_user_secret[0].secret_arn
    }
  }

  depends_on = [aws_cloudwatch_log_group.lambda, aws_iam_role_policy_attachment.procesar_cv_vpc, aws_vpc_endpoint.secretsmanager]
}

resource "aws_lambda_event_source_mapping" "procesar_cv" {
  event_source_arn                   = aws_sqs_queue.cv.arn
  function_name                      = aws_lambda_function.procesar_cv.arn
  batch_size                         = 5
  maximum_batching_window_in_seconds = 5
  function_response_types            = ["ReportBatchItemFailures"]
}
