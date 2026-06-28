variable "ecs_instance_profile_name" {
  description = "Pre-created IAM instance profile for ECS EC2 (create once in AWS Console)"
  type        = string
  default     = "ecsInstanceRole"
}

variable "create_ecs_instance_profile" {
  description = "Create IAM role/profile via Terraform (requires iam:CreateRole permission)"
  type        = bool
  default     = false
}

resource "aws_iam_role" "ecs_instance" {
  count = var.create_ecs_instance_profile ? 1 : 0

  name = "${var.project_name}-ecs-instance-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Service = "ec2.amazonaws.com"
      }
      Action = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_instance" {
  count = var.create_ecs_instance_profile ? 1 : 0

  role       = aws_iam_role.ecs_instance[0].name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role"
}

resource "aws_iam_instance_profile" "ecs_instance" {
  count = var.create_ecs_instance_profile ? 1 : 0

  name = "${var.project_name}-ecs-instance-profile"
  role = aws_iam_role.ecs_instance[0].name
}

locals {
  ecs_instance_profile = var.create_ecs_instance_profile ? aws_iam_instance_profile.ecs_instance[0].name : var.ecs_instance_profile_name
}
