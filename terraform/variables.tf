variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Resource name prefix"
  type        = string
  default     = "flash-sale"
}

variable "ecr_repository" {
  type    = string
  default = "flash-sale-voucher-platform"
}

variable "ecs_cluster_name" {
  type    = string
  default = "flash-sale-cluster"
}

variable "ecs_service_name" {
  type    = string
  default = "flash-sale-service"
}

variable "ecs_task_family" {
  type    = string
  default = "flash-sale-voucher-platform-ec2"
}

variable "rds_instance_id" {
  type    = string
  default = "flash-sale-mysql"
}

variable "rds_username" {
  type    = string
  default = "admin"
}

variable "rds_password" {
  description = "RDS master password (set in terraform.tfvars, do not commit)"
  type        = string
  sensitive   = true
}

variable "rds_allocated_storage" {
  type    = number
  default = 20
}

variable "app_image_tag" {
  description = "Docker image tag pushed to ECR (Jenkins updates via deploy script)"
  type        = string
  default     = "latest"
}

variable "mysql_admin_cidr" {
  description = "CIDR allowed to import SQL into RDS (demo: restrict to your IP)"
  type        = string
  default     = "0.0.0.0/0"
}

variable "app_port_cidr" {
  description = "CIDR allowed to access the app on port 8081"
  type        = string
  default     = "0.0.0.0/0"
}

variable "enable_ssh_ingress" {
  type    = bool
  default = true
}

variable "create_ecs_service" {
  description = "Create ECS service (requires image in ECR; set false on first apply if needed)"
  type        = bool
  default     = true
}
