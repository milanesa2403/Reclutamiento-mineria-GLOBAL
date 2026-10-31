\# Persona 4: Infraestructura de cómputo con Amazon EC2



\## Objetivo



Implementar el componente de cómputo de la Plataforma Global de Reclutamiento Minero mediante Terraform y Amazon EC2, utilizando Amazon Linux 2023 y Apache como servidor web.



\## Archivo desarrollado



`terraform/compute.tf`



\## Implementación



Se desarrolló la configuración de una instancia EC2 con las siguientes características:



\- \*\*AMI:\*\* selección automática de una imagen oficial de Amazon Linux 2023.

\- \*\*Tipo de instancia:\*\* utiliza el tipo definido mediante la variable `instance\\\_type`.

\- \*\*Red:\*\* asociación con la subred pública definida en `network.tf`.

\- \*\*Seguridad:\*\* asociación con el Security Group definido en `security.tf`.

\- \*\*Dirección IP:\*\* asignación de una dirección IP pública.

\- \*\*Apache:\*\* instalación, habilitación e inicio automático mediante `user\\\_data`.

\- \*\*Página web:\*\* creación de una página HTML de prueba para verificar el funcionamiento del servidor.

\- \*\*Etiquetas:\*\* identificación de la instancia mediante el nombre del proyecto.



\## Funcionamiento



La instancia EC2 funciona como servidor web de la plataforma. Apache permite atender las solicitudes HTTP de los usuarios y mostrar la página de prueba desplegada.



La configuración utiliza los recursos de red y seguridad definidos por los demás integrantes del equipo.



\## Dependencias



\- `terraform/main.tf`: configuración del proveedor AWS.

\- `terraform/variables.tf`: variables y nombres de los recursos.

\- `terraform/network.tf`: VPC, subred pública e Internet Gateway.

\- `terraform/security.tf`: grupo de seguridad para permitir tráfico HTTP.



\## Validación



Se realizaron las siguientes comprobaciones:



\- `terraform fmt`: formato de los archivos Terraform.

\- `terraform init -backend=false`: inicialización local sin configurar el backend remoto.

\- `terraform validate`: validación de la configuración.



\*\*Resultado obtenido:\*\*



`Success! The configuration is valid.`



\## Resultado esperado



Contar con una instancia EC2 configurada mediante Terraform, capaz de ejecutar Apache y mostrar una página web de prueba a través de HTTP.



El despliegue definitivo se realizará mediante el workflow de GitHub Actions después de integrar los cambios del equipo.

