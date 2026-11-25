variable "aws_region" {
  description = "Region principal del proyecto"
  type        = string
  default     = "us-east-2"
}

variable "replica_region" {
  description = "Region secundaria donde se copia el bucket de CVs"
  type        = string
  default     = "sa-east-1"
}

variable "environment" {
  description = "Nombre del entorno"
  type        = string
  default     = "dev"
}

variable "name_prefix" {
  description = "Prefijo de los nombres de los recursos"
  type        = string
  default     = "minero"
}

variable "vpc_cidr" {
  description = "Bloque CIDR de la VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "instance_type" {
  description = "Tipo de instancia EC2"
  type        = string
  default     = "t3.micro"
}

variable "asg_min_size" {
  description = "Numero minimo de instancias"
  type        = number
  default     = 1
}

variable "asg_max_size" {
  description = "Numero maximo de instancias"
  type        = number
  default     = 3
}

variable "asg_desired_capacity" {
  description = "Numero inicial de instancias"
  type        = number
  default     = 2
}

variable "cpu_scale_target" {
  description = "Porcentaje de CPU a partir del cual se agrega una instancia"
  type        = number
  default     = 70
}

variable "db_instance_class" {
  description = "Clase de instancia de RDS"
  type        = string
  default     = "db.t4g.micro"
}

variable "db_engine_version" {
  description = "Version de PostgreSQL"
  type        = string
  default     = "16"
}

variable "db_backup_days" {
  description = "Dias de respaldos automaticos de RDS"
  type        = number
  default     = 7
}

variable "db_deletion_protection" {
  description = "Proteccion contra borrado de RDS"
  type        = bool
  default     = true
}

variable "db_max_connections_alarm" {
  description = "Conexiones de RDS que disparan la alarma"
  type        = number
  default     = 64
}

variable "deletion_protection" {
  description = "Proteccion contra borrado de los buckets"
  type        = bool
  default     = false
}

variable "alb_deletion_protection" {
  description = "Proteccion contra borrado del ALB"
  type        = bool
  default     = true
}

variable "alert_email" {
  description = "Correo del equipo que recibe las alarmas"
  type        = string
  default     = ""
}

variable "log_retention_days" {
  description = "Dias de retencion de los logs de CloudWatch"
  type        = number
  default     = 365
}

locals {
  name       = "${var.name_prefix}-${var.environment}"
  account_id = data.aws_caller_identity.current.account_id
  cvs_bucket = "${local.name}-cvs-${local.account_id}"
  azs        = ["${var.aws_region}a", "${var.aws_region}b"]
  has_email  = var.alert_email != ""
  tags = {
    Proyecto      = "Reclutamiento Minero Global"
    Entorno       = var.environment
    Grupo         = "G3"
    GestionadoPor = "Terraform"
  }
}

variable "waf_rate_limit" {
  description = "Solicitudes por IP cada 5 minutos antes de responder 429"
  type        = number
  default     = 300
}

variable "dominio" {
  description = "Dominio propio del DNS gratuito, por ejemplo minero-g3.mooo.com; vacio usa la direccion de CloudFront"
  type        = string
  default     = ""
}

variable "certificados_validados" {
  description = "Poner en true cuando los CNAME de validacion de ACM ya esten creados en el DNS gratuito"
  type        = bool
  default     = false
}
