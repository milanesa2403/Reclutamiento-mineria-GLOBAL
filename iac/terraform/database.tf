resource "aws_db_subnet_group" "main" {
  name       = "${local.name}-db"
  subnet_ids = aws_subnet.private[*].id

  tags = {
    Name = "${local.name}-db"
  }
}

resource "aws_db_parameter_group" "main" {
  name   = "${local.name}-postgres16"
  family = "postgres16"

  parameter {
    name  = "rds.force_ssl"
    value = "1"
  }

  parameter {
    name  = "log_statement"
    value = "ddl"
  }

  parameter {
    name  = "log_min_duration_statement"
    value = "1000"
  }

  parameter {
    name  = "log_connections"
    value = "1"
  }

  parameter {
    name  = "log_disconnections"
    value = "1"
  }

  parameter {
    name  = "statement_timeout"
    value = "5000"
  }
}

resource "aws_iam_role" "rds_monitoring" {
  name = "${local.name}-rds-monitoring"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "monitoring.rds.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "rds_monitoring" {
  role       = aws_iam_role.rds_monitoring.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole"
}

resource "aws_cloudwatch_log_group" "rds" {
  for_each          = toset(["postgresql", "upgrade"])
  name              = "/aws/rds/instance/${local.name}-postgres/${each.value}"
  retention_in_days = var.log_retention_days
  kms_key_id        = aws_kms_key.main.arn
}

# CKV_AWS_293: la base de datos se podia borrar por error
resource "aws_db_instance" "main" {
  #checkov:skip=CKV_AWS_157:Una sola zona por la capa gratuita; si falla se restaura desde el respaldo en la zona B
  identifier                            = "${local.name}-postgres"
  availability_zone                     = local.azs[0]
  engine                                = "postgres"
  engine_version                        = var.db_engine_version
  instance_class                        = var.db_instance_class
  allocated_storage                     = 20
  max_allocated_storage                 = 100
  storage_type                          = "gp3"
  storage_encrypted                     = true
  kms_key_id                            = aws_kms_key.main.arn
  db_name                               = "reclutamiento"
  username                              = "app_reclutamiento"
  manage_master_user_password           = true
  master_user_secret_kms_key_id         = aws_kms_key.main.arn
  multi_az                              = false
  db_subnet_group_name                  = aws_db_subnet_group.main.name
  vpc_security_group_ids                = [aws_security_group.db.id]
  parameter_group_name                  = aws_db_parameter_group.main.name
  publicly_accessible                   = false
  backup_retention_period               = var.db_backup_days
  backup_window                         = "03:00-04:00"
  maintenance_window                    = "sun:08:30-sun:09:30"
  copy_tags_to_snapshot                 = true
  deletion_protection                   = var.db_deletion_protection
  skip_final_snapshot                   = !var.db_deletion_protection
  final_snapshot_identifier             = "${local.name}-postgres-final"
  iam_database_authentication_enabled   = true
  auto_minor_version_upgrade            = true
  performance_insights_enabled          = true
  performance_insights_kms_key_id       = aws_kms_key.main.arn
  performance_insights_retention_period = 7
  monitoring_interval                   = 60
  monitoring_role_arn                   = aws_iam_role.rds_monitoring.arn
  enabled_cloudwatch_logs_exports       = ["postgresql", "upgrade"]
  ca_cert_identifier                    = "rds-ca-rsa2048-g1"

  tags = {
    Name = "${local.name}-postgres"
  }

  depends_on = [aws_cloudwatch_log_group.rds]
}

resource "aws_db_instance" "restaurada" {
  #checkov:skip=CKV_AWS_157:Es la copia de recuperacion en la otra zona que se crea solo cuando falla la principal
  count                                 = var.restaurar_bd ? 1 : 0
  identifier                            = "${local.name}-postgres-restaurada"
  instance_class                        = var.db_instance_class
  availability_zone                     = local.azs[1]
  storage_type                          = "gp3"
  storage_encrypted                     = true
  kms_key_id                            = aws_kms_key.main.arn
  manage_master_user_password           = true
  master_user_secret_kms_key_id         = aws_kms_key.main.arn
  multi_az                              = false
  db_subnet_group_name                  = aws_db_subnet_group.main.name
  vpc_security_group_ids                = [aws_security_group.db.id]
  parameter_group_name                  = aws_db_parameter_group.main.name
  publicly_accessible                   = false
  backup_retention_period               = var.db_backup_days
  backup_window                         = "03:00-04:00"
  copy_tags_to_snapshot                 = true
  deletion_protection                   = false
  skip_final_snapshot                   = true
  iam_database_authentication_enabled   = true
  auto_minor_version_upgrade            = true
  performance_insights_enabled          = true
  performance_insights_kms_key_id       = aws_kms_key.main.arn
  performance_insights_retention_period = 7
  monitoring_interval                   = 60
  monitoring_role_arn                   = aws_iam_role.rds_monitoring.arn
  enabled_cloudwatch_logs_exports       = ["postgresql", "upgrade"]
  ca_cert_identifier                    = "rds-ca-rsa2048-g1"

  restore_to_point_in_time {
    source_db_instance_identifier = aws_db_instance.main.identifier
    use_latest_restorable_time    = true
  }

  tags = {
    Name = "${local.name}-postgres-restaurada"
  }
}

locals {
  db_activa = var.restaurar_bd ? aws_db_instance.restaurada[0] : aws_db_instance.main
}

output "rds_activa" {
  value = local.db_activa.address
}
