resource "aws_cloudwatch_log_group" "ecs" {
  name              = local.log_group
  retention_in_days = 7
}
