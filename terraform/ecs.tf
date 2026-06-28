resource "aws_ecs_cluster" "main" {
  name = var.ecs_cluster_name
}

resource "aws_ecs_task_definition" "app" {
  family                   = var.ecs_task_family
  network_mode             = "bridge"
  requires_compatibilities = ["EC2"]

  container_definitions = jsonencode([
    {
      name      = "redis"
      image     = "redis:6.2-alpine"
      memory    = 64
      essential = true
      command   = ["redis-server", "--requirepass", "root"]
      portMappings = [{
        containerPort = 6379
        hostPort      = 6379
        protocol      = "tcp"
      }]
    },
    {
      name      = "flash-sale-app"
      image     = local.ecr_image
      memory    = 256
      essential = true
      links     = ["redis:redis"]
      dependsOn = [{
        containerName = "redis"
        condition     = "START"
      }]
      portMappings = [{
        containerPort = 8081
        hostPort      = 8081
        protocol      = "tcp"
      }]
      environment = [
        {
          name  = "SPRING_DATASOURCE_URL"
          value = "jdbc:mysql://${aws_db_instance.mysql.address}:3306/hmdp?useSSL=false&serverTimezone=UTC"
        },
        { name = "SPRING_DATASOURCE_USERNAME", value = var.rds_username },
        { name = "SPRING_DATASOURCE_PASSWORD", value = var.rds_password },
        { name = "SPRING_REDIS_HOST", value = "redis" },
        { name = "SPRING_REDIS_PORT", value = "6379" },
        { name = "SPRING_REDIS_PASSWORD", value = "root" }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.ecs.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])

  lifecycle {
    ignore_changes = [container_definitions]
  }
}

resource "aws_ecs_service" "app" {
  count = var.create_ecs_service ? 1 : 0

  name            = var.ecs_service_name
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = 1
  launch_type     = "EC2"

  deployment_minimum_healthy_percent = 0
  deployment_maximum_percent         = 100

  lifecycle {
    ignore_changes = [task_definition, desired_count]
  }

  depends_on = [
    aws_instance.ecs_host,
    aws_db_instance.mysql
  ]
}
