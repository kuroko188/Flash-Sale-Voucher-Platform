output "aws_region" {
  value = var.aws_region
}

output "aws_account_id" {
  value = data.aws_caller_identity.current.account_id
}

output "ecr_repository_url" {
  value = aws_ecr_repository.app.repository_url
}

output "ecr_image_uri" {
  value = local.ecr_image
}

output "rds_endpoint" {
  value = aws_db_instance.mysql.address
}

output "rds_username" {
  value = var.rds_username
}

output "mysql_engine_version" {
  value = local.mysql57_version
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  value = var.ecs_service_name
}

output "ecs_task_family" {
  value = var.ecs_task_family
}

output "ecs_host_public_ip" {
  value = aws_instance.ecs_host.public_ip
}

output "app_url" {
  value = "http://${aws_instance.ecs_host.public_ip}:8081"
}

output "rds_password" {
  value     = var.rds_password
  sensitive = true
}

output "import_schema_commands" {
  value = <<-EOT
    RDS_HOST=${aws_db_instance.mysql.address}
    mysql -h $RDS_HOST -u ${var.rds_username} -p hmdp < hmdp.sql
    mysql -h $RDS_HOST -u ${var.rds_username} -p hmdp < scripts/seed-english-data.sql
  EOT
}

output "teardown_command" {
  value = "cd terraform && terraform destroy"
}
