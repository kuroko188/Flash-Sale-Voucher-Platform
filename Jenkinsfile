pipeline {
    agent any

    environment {
        AWS_REGION        = "${env.AWS_REGION ?: 'us-east-1'}"
        ECR_REPOSITORY    = "${env.ECR_REPOSITORY ?: 'flash-sale-voucher-platform'}"
        ECS_CLUSTER       = "${env.ECS_CLUSTER ?: 'flash-sale-cluster'}"
        ECS_SERVICE       = "${env.ECS_SERVICE ?: 'flash-sale-service'}"
        ECS_TASK_FAMILY   = "${env.ECS_TASK_FAMILY ?: 'flash-sale-voucher-platform'}"
        IMAGE_TAG         = "${env.BUILD_NUMBER ?: 'latest'}"
    }

    options {
        timestamps()
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
                    docker compose -f docker-compose.ci.yml up -d mysql redis
                    for i in $(seq 1 30); do
                      docker compose -f docker-compose.ci.yml exec -T mysql mysqladmin ping -h 127.0.0.1 -proot && break
                      sleep 5
                    done
                    mvn -B test -Pintegration
                '''
            }
            post {
                always {
                    sh 'docker compose -f docker-compose.ci.yml down -v || true'
                }
            }
        }

        stage('Build Docker Image') {
            when {
                branch 'main'
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
                branch 'main'
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
                branch 'main'
            }
            steps {
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

    post {
        always {
            cleanWs()
        }
        success {
            echo 'Pipeline completed successfully.'
        }
        failure {
            echo 'Pipeline failed. Check unit test, coverage, or deployment logs.'
        }
    }
}
