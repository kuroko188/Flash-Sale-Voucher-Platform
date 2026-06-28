pipeline {
    agent any

    environment {
        AWS_REGION        = "${env.AWS_REGION ?: 'us-east-1'}"
        ECR_REPOSITORY    = "${env.ECR_REPOSITORY ?: 'flash-sale-voucher-platform'}"
        ECS_CLUSTER       = "${env.ECS_CLUSTER ?: 'flash-sale-cluster'}"
        ECS_SERVICE       = "${env.ECS_SERVICE ?: 'flash-sale-service'}"
        ECS_TASK_FAMILY   = "${env.ECS_TASK_FAMILY ?: 'flash-sale-voucher-platform-ec2'}"
        DEPLOY_TARGET     = "${env.DEPLOY_TARGET ?: 'free-tier-ec2'}"
        IMAGE_TAG         = "${env.BUILD_NUMBER ?: 'latest'}"
        SKIP_DEPLOY       = "${env.SKIP_DEPLOY ?: 'true'}"
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Unit Tests & Coverage') {
            steps {
                sh 'mvn -B clean verify'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    jacoco execPattern: 'target/jacoco.exec',
                          classPattern: 'target/classes',
                          sourcePattern: 'src/main/java'
                }
            }
        }

        stage('Integration Tests') {
            steps {
                sh '''
                    export SPRING_DATASOURCE_USERNAME=root
                    export SPRING_DATASOURCE_PASSWORD=root
                    export SPRING_REDIS_PASSWORD=root

                    if docker ps --format '{{.Names}}' | grep -qx 'mysql57' && docker ps --format '{{.Names}}' | grep -qx 'redis'; then
                      echo "Using host mysql57/redis containers via host.docker.internal"
                      export SPRING_DATASOURCE_URL='jdbc:mysql://host.docker.internal:3306/hmdp?useSSL=false&serverTimezone=UTC'
                      export SPRING_REDIS_HOST=host.docker.internal
                      export SPRING_REDIS_PORT=6379
                    elif docker compose version >/dev/null 2>&1; then
                      docker rm -f hmdp-ci-redis hmdp-ci-mysql hmdp-ci-app 2>/dev/null || true
                      docker compose -f docker-compose.ci.yml down -v 2>/dev/null || true
                      docker compose -f docker-compose.ci.yml up -d mysql redis
                      for i in $(seq 1 30); do
                        docker compose -f docker-compose.ci.yml exec -T mysql mysqladmin ping -h 127.0.0.1 -proot && break
                        sleep 5
                      done
                      export SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3307/hmdp?useSSL=false&serverTimezone=UTC'
                      export SPRING_REDIS_HOST=127.0.0.1
                      export SPRING_REDIS_PORT=6380
                    else
                      echo "docker compose unavailable, using host MySQL/Redis"
                      export SPRING_DATASOURCE_URL='jdbc:mysql://host.docker.internal:3306/hmdp?useSSL=false&serverTimezone=UTC'
                      export SPRING_REDIS_HOST=host.docker.internal
                      export SPRING_REDIS_PORT=6379
                    fi

                    mvn -B test -Pintegration
                '''
            }
            post {
                always {
                    sh 'docker compose -f docker-compose.ci.yml down -v 2>/dev/null || true'
                }
            }
        }

        stage('Build Docker Image') {
            when {
                expression { env.SKIP_DEPLOY == 'false' }
            }
            steps {
                script {
                    def accountId = sh(script: 'aws sts get-caller-identity --query Account --output text', returnStdout: true).trim()
                    env.ECR_REGISTRY = "${accountId}.dkr.ecr.${AWS_REGION}.amazonaws.com"
                    env.IMAGE_URI = "${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"
                }
                sh 'docker build -t ${IMAGE_URI} .'
            }
        }

        stage('Push to ECR') {
            when {
                expression { env.SKIP_DEPLOY == 'false' }
            }
            steps {
                sh '''
                    aws ecr get-login-password --region ${AWS_REGION} | \
                      docker login --username AWS --password-stdin ${ECR_REGISTRY}
                    docker push ${IMAGE_URI}
                '''
            }
        }

        stage('Deploy to ECS') {
            when {
                expression { env.SKIP_DEPLOY == 'false' }
            }
            steps {
                script {
                    if (env.DEPLOY_TARGET == 'free-tier-ec2') {
                        sh '''
                            chmod +x scripts/deploy-ecs-ec2-free-tier.sh
                            ./scripts/deploy-ecs-ec2-free-tier.sh \
                              --region ${AWS_REGION} \
                              --cluster ${ECS_CLUSTER} \
                              --service ${ECS_SERVICE} \
                              --task-family ${ECS_TASK_FAMILY} \
                              --image ${IMAGE_URI} \
                              --from-terraform
                        '''
                    } else {
                        sh '''
                            chmod +x scripts/deploy-ecs.sh
                            ./scripts/deploy-ecs.sh \
                              --region ${AWS_REGION} \
                              --cluster ${ECS_CLUSTER} \
                              --service ${ECS_SERVICE} \
                              --task-family ${ECS_TASK_FAMILY} \
                              --image ${IMAGE_URI}
                        '''
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully.'
        }
        failure {
            echo 'Pipeline failed. Check unit test, coverage, or deployment logs.'
        }
    }
}
