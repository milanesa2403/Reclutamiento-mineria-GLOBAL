locals {
  usar_dominio  = var.dominio != ""
  dominio_listo = local.usar_dominio && var.certificados_validados
  dominio_api   = "api.${var.dominio}"
  frontend_url  = local.dominio_listo ? "https://${var.dominio}" : "https://${aws_cloudfront_distribution.frontend.domain_name}"
}

resource "aws_acm_certificate" "cloudfront" {
  provider          = aws.us_east_1
  count             = local.usar_dominio ? 1 : 0
  domain_name       = var.dominio
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_acm_certificate" "api" {
  count             = local.usar_dominio ? 1 : 0
  domain_name       = local.dominio_api
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}
