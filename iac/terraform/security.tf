data "aws_ec2_managed_prefix_list" "cloudfront" {
  name = "com.amazonaws.global.cloudfront.origin-facing"
}

resource "aws_security_group" "alb" {
  name        = "${local.name}-alb-sg"
  description = "Application Load Balancer publico"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "HTTP solo desde CloudFront"
    from_port       = 80
    to_port         = 80
    protocol        = "tcp"
    prefix_list_ids = [data.aws_ec2_managed_prefix_list.cloudfront.id]
  }

  ingress {
    description = "HTTPS de los usuarios"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "HTTPS hacia las instancias EC2"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = aws_subnet.public[*].cidr_block
  }

  tags = {
    Name = "${local.name}-alb-sg"
  }
}

resource "aws_security_group" "web" {
  name        = "${local.name}-web-sg"
  description = "Instancias EC2 web y API"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "HTTPS solo desde el ALB"
    from_port       = 443
    to_port         = 443
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    description = "HTTPS para descargar paquetes e imagenes"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "PostgreSQL hacia las subredes privadas"
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = aws_subnet.private[*].cidr_block
  }

  tags = {
    Name = "${local.name}-web-sg"
  }
}

resource "aws_security_group" "db" {
  name        = "${local.name}-db-sg"
  description = "RDS PostgreSQL solo desde las EC2"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "PostgreSQL desde las EC2"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.web.id]
  }

  tags = {
    Name = "${local.name}-db-sg"
  }
}
