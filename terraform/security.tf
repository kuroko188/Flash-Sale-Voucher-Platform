resource "aws_security_group" "app" {
  name        = "${var.project_name}-free-tier-sg"
  description = "Flash sale free tier demo"
  vpc_id      = data.aws_vpc.default.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-free-tier-sg"
  }
}

resource "aws_vpc_security_group_ingress_rule" "app_http" {
  security_group_id = aws_security_group.app.id
  cidr_ipv4         = var.app_port_cidr
  from_port         = 8081
  to_port           = 8081
  ip_protocol       = "tcp"
  description       = "Spring Boot app"
}

resource "aws_vpc_security_group_ingress_rule" "app_ssh" {
  count = var.enable_ssh_ingress ? 1 : 0

  security_group_id = aws_security_group.app.id
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 22
  to_port           = 22
  ip_protocol       = "tcp"
  description       = "SSH (demo only)"
}

resource "aws_vpc_security_group_ingress_rule" "mysql_from_app" {
  security_group_id            = aws_security_group.app.id
  referenced_security_group_id = aws_security_group.app.id
  from_port                    = 3306
  to_port                      = 3306
  ip_protocol                  = "tcp"
  description                  = "MySQL from ECS host"
}

resource "aws_vpc_security_group_ingress_rule" "mysql_admin" {
  security_group_id = aws_security_group.app.id
  cidr_ipv4         = var.mysql_admin_cidr
  from_port         = 3306
  to_port           = 3306
  ip_protocol       = "tcp"
  description       = "MySQL admin import (restrict in production)"
}
