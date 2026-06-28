data "aws_caller_identity" "current" {}

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }

  filter {
    name   = "default-for-az"
    values = ["true"]
  }
}

data "aws_ssm_parameter" "ecs_ami" {
  name = "/aws/service/ecs/optimized-ami/amazon-linux-2/recommended/image_id"
}

data "aws_rds_engine_version" "mysql57" {
  engine = "mysql"
  preferred_versions = [
    "5.7.44-rds.20260521",
    "5.7.44-rds.20260212",
    "5.7.44-rds.20251212",
    "5.7.44-rds.20250818",
    "5.7.44-rds.20250508",
    "5.7.44-rds.20250213",
  ]
}

locals {
  mysql57_version = data.aws_rds_engine_version.mysql57.version

  ecr_image = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.aws_region}.amazonaws.com/${var.ecr_repository}:${var.app_image_tag}"
  log_group = "/ecs/${var.ecr_repository}"
}
