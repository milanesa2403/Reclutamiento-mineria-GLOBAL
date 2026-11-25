locals {
  kms_regions = {
    main    = var.aws_region
    replica = var.replica_region
  }

  kms_policies = {
    for key, region in local.kms_regions : key => jsonencode({
      Version = "2012-10-17"
      Statement = [
        {
          Sid       = "AdministracionCuenta"
          Effect    = "Allow"
          Principal = { AWS = "arn:aws:iam::${local.account_id}:root" }
          Action    = "kms:*"
          Resource  = "*"
        },
        {
          Sid       = "CloudWatchLogs"
          Effect    = "Allow"
          Principal = { Service = "logs.${region}.amazonaws.com" }
          Action    = ["kms:Encrypt*", "kms:Decrypt*", "kms:ReEncrypt*", "kms:GenerateDataKey*", "kms:Describe*"]
          Resource  = "*"
          Condition = {
            ArnLike = {
              "kms:EncryptionContext:aws:logs:arn" = "arn:aws:logs:${region}:${local.account_id}:*"
            }
          }
        },
        {
          Sid    = "ServiciosAws"
          Effect = "Allow"
          Principal = {
            Service = [
              "s3.amazonaws.com",
              "sns.amazonaws.com",
              "cloudwatch.amazonaws.com",
              "events.amazonaws.com",
              "cloudtrail.amazonaws.com"
            ]
          }
          Action   = ["kms:Decrypt", "kms:GenerateDataKey*", "kms:DescribeKey"]
          Resource = "*"
        },
        {
          Sid       = "ColaCv"
          Effect    = "Allow"
          Principal = { Service = "sqs.amazonaws.com" }
          Action    = ["kms:Decrypt", "kms:GenerateDataKey*", "kms:DescribeKey"]
          Resource  = "*"
        },
        {
          Sid       = "VolumenesAutoScaling"
          Effect    = "Allow"
          Principal = { AWS = "*" }
          Action    = ["kms:Encrypt", "kms:Decrypt", "kms:ReEncrypt*", "kms:GenerateDataKey*", "kms:DescribeKey", "kms:CreateGrant"]
          Resource  = "*"
          Condition = {
            StringEquals = {
              "kms:CallerAccount" = local.account_id
              "aws:PrincipalArn"  = "arn:aws:iam::${local.account_id}:role/aws-service-role/autoscaling.amazonaws.com/AWSServiceRoleForAutoScaling"
            }
          }
        }
      ]
    })
  }
}

resource "aws_kms_key" "main" {
  description             = "Llave principal de ${local.name}"
  enable_key_rotation     = true
  deletion_window_in_days = 7
  policy                  = local.kms_policies["main"]

  tags = {
    Name = "${local.name}-kms"
  }
}

resource "aws_kms_alias" "main" {
  name          = "alias/${local.name}"
  target_key_id = aws_kms_key.main.key_id
}

resource "aws_kms_key" "replica" {
  provider                = aws.replica
  description             = "Llave de ${local.name} para la replica de CVs"
  enable_key_rotation     = true
  deletion_window_in_days = 7
  policy                  = local.kms_policies["replica"]

  tags = {
    Name = "${local.name}-kms-replica"
  }
}

resource "aws_kms_alias" "replica" {
  provider      = aws.replica
  name          = "alias/${local.name}"
  target_key_id = aws_kms_key.replica.key_id
}
