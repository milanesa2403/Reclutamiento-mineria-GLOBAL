resource "aws_ecr_repository" "backend" {
  name                 = "${local.name}-backend"
  image_tag_mutability = "IMMUTABLE"
  force_delete         = !var.deletion_protection

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "KMS"
    kms_key         = aws_kms_key.main.arn
  }
}

resource "aws_ecr_lifecycle_policy" "backend" {
  repository = aws_ecr_repository.backend.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Conservar las 5 imagenes mas recientes"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 5
      }
      action = { type = "expire" }
    }]
  })
}

resource "random_password" "jwt" {
  length  = 48
  special = false
}

resource "aws_ssm_parameter" "jwt" {
  name   = "/${local.name}/backend/jwt-secret"
  type   = "SecureString"
  key_id = aws_kms_key.main.arn
  value  = random_password.jwt.result
}

resource "aws_ssm_parameter" "imagen_backend" {
  #checkov:skip=CKV2_AWS_34:Solo guarda la version de la imagen, no es un dato secreto
  name  = "/${local.name}/backend/imagen"
  type  = "String"
  value = "ninguna"

  lifecycle {
    ignore_changes = [value]
  }
}

output "ecr_repository_url" {
  value = aws_ecr_repository.backend.repository_url
}

output "imagen_parametro" {
  value = aws_ssm_parameter.imagen_backend.name
}
