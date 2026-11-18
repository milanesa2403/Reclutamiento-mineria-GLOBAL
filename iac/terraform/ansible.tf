resource "aws_s3_bucket" "ansible" {
  bucket        = "${local.name}-ansible-${local.account_id}"
  force_destroy = true
}

resource "aws_s3_bucket_public_access_block" "ansible" {
  bucket                  = aws_s3_bucket.ansible.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_ownership_controls" "ansible" {
  bucket = aws_s3_bucket.ansible.id

  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_versioning" "ansible" {
  bucket = aws_s3_bucket.ansible.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "ansible" {
  bucket = aws_s3_bucket.ansible.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_logging" "ansible" {
  bucket        = aws_s3_bucket.ansible.id
  target_bucket = aws_s3_bucket.logs.id
  target_prefix = "s3/ansible/"
}

resource "aws_s3_bucket_lifecycle_configuration" "ansible" {
  bucket = aws_s3_bucket.ansible.id

  rule {
    id     = "temporales"
    status = "Enabled"

    filter {}

    expiration {
      days = 1
    }

    noncurrent_version_expiration {
      noncurrent_days = 1
    }

    abort_incomplete_multipart_upload {
      days_after_initiation = 1
    }
  }
}

resource "aws_s3_bucket_policy" "ansible" {
  bucket = aws_s3_bucket.ansible.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid       = "SoloTls"
      Effect    = "Deny"
      Principal = "*"
      Action    = "s3:*"
      Resource  = [aws_s3_bucket.ansible.arn, "${aws_s3_bucket.ansible.arn}/*"]
      Condition = {
        Bool = { "aws:SecureTransport" = "false" }
      }
    }]
  })

  depends_on = [aws_s3_bucket_public_access_block.ansible]
}

resource "aws_s3_bucket_notification" "ansible" {
  bucket      = aws_s3_bucket.ansible.id
  eventbridge = true
}
