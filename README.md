# Proyecto de Infraestructura sobre Plataforma de Reclutamiento Minero

Proyecto: Plataforma global para reclutamiento de minería con infraestructura escalable.

Hemos desplegado con Terraform la infraestructura del proyecto en AWS usando GitHub Actions. La conexión con AWS es por OIDC y el estado de Terraform se guarda en un bucket S3.

## Ramas

- main: version final donde se encontrara todo el codigo
- development: aqui se une todo y se despliega en AWS
- releases/...: avances de cada integrante, se hace pull request a development

## Workflows

- github-actions-ReclutamientoMineria.yml: pipeline del proyecto, por ahora solo echo
- validar-terraform.yml: fmt y validate en los pull request
- desplegar.yml: apply con push a development
- destruir.yml: destroy manual desde Actions

Fuentes:
https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/trigger-a-workflow
https://github.com/aws-actions/configure-aws-credentials
https://github.com/hashicorp/setup-terraform

## Terraform base

main.tf: version de Terraform, provider de AWS y backend S3 (el estado se guarda en lab09/dev/terraform.tfstate).
variables.tf: region, ambiente, prefijo de nombres y tipo de instancia.
backend.hcl.example: plantilla con el nombre del bucket.

Fuentes:
https://registry.terraform.io/providers/hashicorp/aws/latest/docs
https://developer.hashicorp.com/terraform/language/backend/s3
https://developer.hashicorp.com/terraform/language/values/variables

## Security group

security.tf: deja entrar HTTP por el puerto 80.

Fuentes:
https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group
