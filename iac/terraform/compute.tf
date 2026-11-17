data "aws_ssm_parameter" "al2023" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

resource "aws_cloudwatch_log_group" "web" {
  name              = "/plataforma/${local.name}/web"
  retention_in_days = var.log_retention_days
  kms_key_id        = aws_kms_key.main.arn
}

resource "aws_iam_role" "web" {
  name = "${local.name}-web-ec2"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "web_ssm" {
  role       = aws_iam_role.web.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_role_policy" "web" {
  name = "permisos-web"
  role = aws_iam_role.web.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid      = "LogsContenedores"
        Effect   = "Allow"
        Action   = ["logs:CreateLogStream", "logs:PutLogEvents", "logs:DescribeLogStreams"]
        Resource = "${aws_cloudwatch_log_group.web.arn}:*"
      },
      {
        Sid      = "CvsEnS3"
        Effect   = "Allow"
        Action   = ["s3:PutObject", "s3:GetObject"]
        Resource = "${aws_s3_bucket.cvs.arn}/cvs/*"
      },
      {
        Sid      = "SecretoBaseDatos"
        Effect   = "Allow"
        Action   = "secretsmanager:GetSecretValue"
        Resource = aws_db_instance.main.master_user_secret[0].secret_arn
      },
      {
        Sid      = "TokenEcr"
        Effect   = "Allow"
        Action   = "ecr:GetAuthorizationToken"
        Resource = "*"
      },
      {
        Sid      = "ImagenBackend"
        Effect   = "Allow"
        Action   = ["ecr:BatchGetImage", "ecr:GetDownloadUrlForLayer", "ecr:BatchCheckLayerAvailability"]
        Resource = aws_ecr_repository.backend.arn
      },
      {
        Sid      = "ParametrosBackend"
        Effect   = "Allow"
        Action   = "ssm:GetParameter"
        Resource = [aws_ssm_parameter.jwt.arn, aws_ssm_parameter.imagen_backend.arn]
      },
      {
        Sid      = "Cifrado"
        Effect   = "Allow"
        Action   = ["kms:Decrypt", "kms:GenerateDataKey"]
        Resource = aws_kms_key.main.arn
      }
    ]
  })
}

resource "aws_iam_instance_profile" "web" {
  name = "${local.name}-web-ec2"
  role = aws_iam_role.web.name
}

resource "aws_launch_template" "web" {
  name                   = "${local.name}-web"
  image_id               = data.aws_ssm_parameter.al2023.value
  instance_type          = var.instance_type
  update_default_version = true
  ebs_optimized          = true

  iam_instance_profile {
    arn = aws_iam_instance_profile.web.arn
  }

  network_interfaces {
    associate_public_ip_address = true
    security_groups             = [aws_security_group.web.id]
    delete_on_termination       = true
  }

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 1
  }

  monitoring {
    enabled = true
  }

  block_device_mappings {
    device_name = "/dev/xvda"

    ebs {
      volume_size           = 30
      volume_type           = "gp3"
      encrypted             = true
      kms_key_id            = aws_kms_key.main.arn
      delete_on_termination = true
    }
  }

  user_data = base64encode(templatefile("${path.module}/templates/user_data.sh.tftpl", {
    region    = var.aws_region
    log_group = aws_cloudwatch_log_group.web.name
  }))

  tag_specifications {
    resource_type = "instance"

    tags = merge(local.tags, {
      Name = "${local.name}-web"
    })
  }

  tag_specifications {
    resource_type = "volume"

    tags = merge(local.tags, {
      Name = "${local.name}-web"
    })
  }
}

resource "aws_autoscaling_group" "web" {
  name                      = "${local.name}-web-asg"
  vpc_zone_identifier       = aws_subnet.public[*].id
  min_size                  = var.asg_min_size
  max_size                  = var.asg_max_size
  desired_capacity          = var.asg_desired_capacity
  health_check_type         = "ELB"
  health_check_grace_period = 300
  target_group_arns         = [aws_lb_target_group.web.arn]
  default_instance_warmup   = 120
  enabled_metrics           = ["GroupInServiceInstances", "GroupDesiredCapacity", "GroupPendingInstances", "GroupTerminatingInstances"]

  launch_template {
    id      = aws_launch_template.web.id
    version = aws_launch_template.web.latest_version
  }

  instance_refresh {
    strategy = "Rolling"

    preferences {
      min_healthy_percentage = 50
      instance_warmup        = 120
      auto_rollback          = true
    }
  }

  dynamic "tag" {
    for_each = merge(local.tags, { Name = "${local.name}-web" })

    content {
      key                 = tag.key
      value               = tag.value
      propagate_at_launch = true
    }
  }
}

resource "aws_autoscaling_policy" "cpu" {
  name                   = "${local.name}-cpu-${var.cpu_scale_target}"
  autoscaling_group_name = aws_autoscaling_group.web.name
  policy_type            = "TargetTrackingScaling"

  target_tracking_configuration {
    target_value = var.cpu_scale_target

    predefined_metric_specification {
      predefined_metric_type = "ASGAverageCPUUtilization"
    }
  }
}
