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
variables.tf: region, ambiente, prefijo de nombres y tipo de instancia.

Fuentes:
https://registry.terraform.io/providers/hashicorp/aws/latest/docs
https://developer.hashicorp.com/terraform/language/backend/s3
https://developer.hashicorp.com/terraform/language/values/variables

## Red
network.tf: VPC, internet gateway, subred publica y tabla de rutas.

Fuentes:
https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/vpc
https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/subnet
https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/route_table

## Security group
security.tf: deja entrar HTTP por el puerto 80.

Fuentes:
https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group

## EC2
compute.tf: instancia t3.micro con Amazon Linux 2023. Con user_data se instala Apache y muestra una pagina.

Fuentes:
https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/instance
https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/user-data.html

## Configuracion AWS y GitHub
- Region us-east-2, MFA, alerta de presupuesto y bucket S3 para el estado
- Proveedor OIDC de GitHub y rol lab09-github-actions-dev (plantillas en iam/)
- Environment dev y variables AWS_REGION, AWS_ROLE_ARN y TF_STATE_BUCKET

outputs.tf: muestra la URL de la pagina (web_url) y el id de la instancia.

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