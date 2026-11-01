output "web_url" {
  value = "http://${aws_instance.web.public_ip}"
}

output "instance_id" {
  value = aws_instance.web.id
}