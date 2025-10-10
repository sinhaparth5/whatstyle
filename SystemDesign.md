# System Design

### High-Level System Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        WA[WhatsApp Users]
        WEB[Web Dashboard]
        API_CLIENT[API Clients]
    end

    subgraph "Load Balancer & Gateway"
        LB[Nginx/AWS ALB<br/>Load Balancer]
        AG[API Gateway<br/>Rate Limiting & Auth]
    end

    subgraph "Java MCP Server Cluster - Auto Scaling"
        MCP1[MCP Server Instance 1<br/>Java 21 Virtual Threads]
        MCP2[MCP Server Instance 2<br/>Java 21 Virtual Threads]
        MCP3[MCP Server Instance 3<br/>Java 21 Virtual Threads]
        MCPN[MCP Server Instance N<br/>Auto-scaled]
    end

    subgraph "Message Queue Layer"
        KAFKA[Apache Kafka<br/>Message Broker]
        REDIS[Redis<br/>Cache & Session]
    end

    subgraph "Python AI Service Cluster - Auto Scaling"
        AI1[AI Service Instance 1<br/>Model Inference]
        AI2[AI Service Instance 2<br/>Model Inference]
        AI3[AI Service Instance 3<br/>Model Inference]
        AIN[AI Service Instance N<br/>Auto-scaled]
    end

    subgraph "Storage Layer"
        PG[(PostgreSQL<br/>Primary DB<br/>User Data & Analytics)]
        PG_REPLICA[(PostgreSQL<br/>Read Replica)]
        S3[AWS S3 / MinIO<br/>Model Storage<br/>Chat Exports]
        VECTOR[(Vector DB<br/>Pinecone/Weaviate<br/>Embeddings)]
    end

    subgraph "Training Pipeline - Async"
        TRAIN_QUEUE[Training Job Queue]
        TRAIN1[Training Worker 1<br/>GPU Instance]
        TRAIN2[Training Worker 2<br/>GPU Instance]
    end

    subgraph "Monitoring & Observability"
        PROM[Prometheus<br/>Metrics]
        GRAF[Grafana<br/>Dashboards]
        JAEGER[Jaeger<br/>Distributed Tracing]
        ELK[ELK Stack<br/>Logging]
    end

    WA --> LB
    WEB --> LB
    API_CLIENT --> LB
    
    LB --> AG
    AG --> MCP1
    AG --> MCP2
    AG --> MCP3
    AG --> MCPN

    MCP1 -.gRPC.-> AI1
    MCP2 -.gRPC.-> AI2
    MCP3 -.gRPC.-> AI3
    MCPN -.gRPC.-> AIN

    MCP1 --> KAFKA
    MCP2 --> KAFKA
    MCP3 --> KAFKA
    MCPN --> KAFKA

    MCP1 --> REDIS
    MCP2 --> REDIS
    MCP3 --> REDIS
    MCPN --> REDIS

    MCP1 --> PG
    MCP2 --> PG
    MCP3 --> PG
    MCPN --> PG

    MCP1 --> PG_REPLICA
    MCP2 --> PG_REPLICA
    MCP3 --> PG_REPLICA
    MCPN --> PG_REPLICA

    AI1 --> S3
    AI2 --> S3
    AI3 --> S3
    AIN --> S3

    AI1 --> VECTOR
    AI2 --> VECTOR
    AI3 --> VECTOR
    AIN --> VECTOR

    KAFKA --> TRAIN_QUEUE
    TRAIN_QUEUE --> TRAIN1
    TRAIN_QUEUE --> TRAIN2

    TRAIN1 --> S3
    TRAIN2 --> S3

    MCP1 --> PROM
    MCP2 --> PROM
    MCP3 --> PROM
    AI1 --> PROM
    AI2 --> PROM

    PROM --> GRAF
    
    MCP1 --> JAEGER
    AI1 --> JAEGER
    
    MCP1 --> ELK
    AI1 --> ELK

    style MCP1 fill:#4CAF50
    style MCP2 fill:#4CAF50
    style MCP3 fill:#4CAF50
    style MCPN fill:#4CAF50
    
    style AI1 fill:#2196F3
    style AI2 fill:#2196F3
    style AI3 fill:#2196F3
    style AIN fill:#2196F3
    
    style TRAIN1 fill:#FF9800
    style TRAIN2 fill:#FF9800
    
    style PG fill:#9C27B0
    style REDIS fill:#F44336
    style KAFKA fill:#607D8B
```

### Chat Message Flow

```mermaid
sequenceDiagram
    participant U as User (WhatsApp)
    participant LB as Load Balancer
    participant MCP as MCP Server (Java)
    participant R as Redis Cache
    participant AI as AI Service (Python)
    participant DB as PostgreSQL
    participant K as Kafka

    U->>LB: Send Message
    LB->>MCP: Route to Available Instance
    
    MCP->>R: Check User Session
    R-->>MCP: Session Data
    
    MCP->>DB: Fetch User Context
    DB-->>MCP: User Profile & History
    
    MCP->>R: Check Response Cache
    alt Cache Hit
        R-->>MCP: Cached Response
        MCP->>U: Return Cached Response (Fast Path)
    else Cache Miss
        MCP->>AI: gRPC GenerateResponse(userId, message, context)
        
        AI->>AI: Load User Model
        AI->>AI: Generate Response
        
        AI-->>MCP: ChatResponse(response, confidence, time)
        
        MCP->>R: Cache Response (TTL: 5min)
        MCP->>K: Publish Analytics Event
        MCP->>DB: Store Conversation (Async)
        
        MCP->>U: Return AI Response
    end
    
    K->>K: Process Analytics Pipeline
```

### Training Flow
```mermaid
sequenceDiagram
    participant U as User
    participant MCP as MCP Server (Java)
    participant S3 as S3 Storage
    participant K as Kafka
    participant TQ as Training Queue
    participant TW as Training Worker (GPU)
    participant AI as AI Service
    participant DB as PostgreSQL

    U->>MCP: Upload WhatsApp Export (.txt)
    MCP->>MCP: Validate File (size, format)
    
    MCP->>S3: Store Raw Chat Data
    S3-->>MCP: Storage URL
    
    MCP->>K: Publish TrainingJob Event
    K->>TQ: Queue Training Job
    
    MCP->>U: Training Started (Job ID)
    
    TQ->>TW: Assign Training Job
    TW->>S3: Download Chat Data
    
    TW->>TW: Parse WhatsApp Format
    TW->>TW: Preprocess Data
    TW->>TW: Fine-tune Model (3 epochs)
    
    TW->>S3: Save Trained Model
    TW->>DB: Update Training Status (Completed)
    
    TW->>K: Publish TrainingComplete Event
    K->>MCP: Notify Training Complete
    
    MCP->>AI: Reload User Model (gRPC)
    AI->>S3: Load New Model
    
    MCP->>U: Training Complete Notification
```

###  High Concurrency Scenario
```mermaid
sequenceDiagram
    participant U1 as User 1
    participant U2 as User 2
    participant UN as User N
    participant LB as Load Balancer
    participant MCP1 as MCP Instance 1
    participant MCP2 as MCP Instance 2
    participant AI1 as AI Instance 1
    participant AI2 as AI Instance 2
    participant R as Redis
    
    par Concurrent Requests
        U1->>LB: Message Request
        U2->>LB: Message Request
        UN->>LB: Message Request
    end
    
    LB->>MCP1: Route User 1 (Round Robin)
    LB->>MCP2: Route User 2 (Round Robin)
    LB->>MCP1: Route User N (Least Connections)
    
    par Parallel Processing
        MCP1->>R: Check Cache (User 1)
        MCP2->>R: Check Cache (User 2)
        MCP1->>R: Check Cache (User N)
    end
    
    R-->>MCP1: Cache Miss
    R-->>MCP2: Cache Miss
    R-->>MCP1: Cache Miss
    
    par Parallel AI Calls
        MCP1->>AI1: gRPC Call (User 1)
        MCP2->>AI2: gRPC Call (User 2)
        MCP1->>AI1: gRPC Call (User N)
    end
    
    par AI Processing
        AI1->>AI1: Process User 1
        AI2->>AI2: Process User 2
        AI1->>AI1: Process User N
    end
    
    AI1-->>MCP1: Response User 1
    AI2-->>MCP2: Response User 2
    AI1-->>MCP1: Response User N
    
    par Return to Users
        MCP1->>U1: Send Response
        MCP2->>U2: Send Response
        MCP1->>UN: Send Response
    end
```

### Component Diagram
```
graph TB
    subgraph "MCP Server - Java 21"
        REST[REST Controllers<br/>@RestController]
        SERVICE[Service Layer<br/>Business Logic]
        GRPC_CLIENT[gRPC Client<br/>AIServiceStub]
        CACHE[Cache Manager<br/>Redis Operations]
        REPO[Repository Layer<br/>JPA/JDBC]
        ANALYTICS[Analytics Service<br/>Metrics Collection]
    end
    
    subgraph "AI Service - Python"
        GRPC_SERVER[gRPC Server<br/>AIServiceServicer]
        CHAT_SVC[Chat Service<br/>Inference Logic]
        TRAIN_SVC[Training Service<br/>Model Training]
        MODEL_MGR[Model Manager<br/>Model Loading/Caching]
        PARSER[WhatsApp Parser<br/>Data Preprocessing]
    end
    
    REST --> SERVICE
    SERVICE --> GRPC_CLIENT
    SERVICE --> CACHE
    SERVICE --> REPO
    SERVICE --> ANALYTICS
    
    GRPC_CLIENT -.gRPC.-> GRPC_SERVER
    
    GRPC_SERVER --> CHAT_SVC
    GRPC_SERVER --> TRAIN_SVC
    CHAT_SVC --> MODEL_MGR
    TRAIN_SVC --> MODEL_MGR
    TRAIN_SVC --> PARSER
    
    style REST fill:#4CAF50
    style GRPC_SERVER fill:#2196F3
    style MODEL_MGR fill:#FF9800
```

### Deployment Diagram
```mermaid
graph TB
    subgraph "Kubernetes Cluster"
        subgraph "Namespace: mcp-server"
            MCP_DEPLOY[Deployment: mcp-server<br/>Replicas: 3-10<br/>HPA: CPU > 70%]
            MCP_SVC[Service: mcp-service<br/>Type: ClusterIP<br/>Port: 8080]
            MCP_INGRESS[Ingress: api.whatstyle.io<br/>TLS/SSL]
        end
        
        subgraph "Namespace: ai-service"
            AI_DEPLOY[Deployment: ai-service<br/>Replicas: 2-8<br/>HPA: Custom Metrics]
            AI_SVC[Service: ai-service<br/>Type: ClusterIP<br/>Port: 50051]
        end
        
        subgraph "Namespace: training"
            TRAIN_JOB[CronJob: model-training<br/>GPU: NVIDIA T4<br/>Node Selector: gpu=true]
        end
        
        subgraph "Namespace: monitoring"
            PROM_DEPLOY[Prometheus<br/>StatefulSet]
            GRAF_DEPLOY[Grafana<br/>Deployment]
        end
    end
    
    subgraph "External Services"
        RDS[(AWS RDS PostgreSQL<br/>Multi-AZ)]
        ELASTICACHE[AWS ElastiCache Redis<br/>Cluster Mode]
        MSK[AWS MSK Kafka<br/>3 Brokers]
        S3_BUCKET[S3 Bucket<br/>Model Storage]
    end
    
    MCP_INGRESS --> MCP_SVC
    MCP_SVC --> MCP_DEPLOY
    MCP_DEPLOY --> AI_SVC
    AI_SVC --> AI_DEPLOY
    
    MCP_DEPLOY --> RDS
    MCP_DEPLOY --> ELASTICACHE
    MCP_DEPLOY --> MSK
    
    AI_DEPLOY --> S3_BUCKET
    TRAIN_JOB --> S3_BUCKET
    
    MCP_DEPLOY --> PROM_DEPLOY
    AI_DEPLOY --> PROM_DEPLOY
    PROM_DEPLOY --> GRAF_DEPLOY
```

### Data Flow Diagram
```mermaid
graph LR
    USER[User Input] --> VALIDATION[Input Validation<br/>& Sanitization]
    VALIDATION --> CONTEXT[Context Enrichment<br/>User Profile + History]
    CONTEXT --> CACHE_CHECK{Cache<br/>Available?}
    
    CACHE_CHECK -->|Yes| CACHE_RESPONSE[Return Cached Response]
    CACHE_CHECK -->|No| AI_INFERENCE[AI Model Inference]
    
    AI_INFERENCE --> RESPONSE_GEN[Response Generation]
    RESPONSE_GEN --> QUALITY_CHECK[Quality Check<br/>Confidence Score]
    
    QUALITY_CHECK -->|High Confidence| CACHE_STORE[Store in Cache]
    QUALITY_CHECK -->|Low Confidence| FALLBACK[Fallback Response]
    
    CACHE_STORE --> PERSIST[Persist to DB]
    FALLBACK --> PERSIST
    
    PERSIST --> ANALYTICS[Analytics Pipeline]
    ANALYTICS --> USER_RESPONSE[Return to User]
    
    CACHE_RESPONSE --> USER_RESPONSE
    
    style AI_INFERENCE fill:#2196F3
    style CACHE_CHECK fill:#F44336
    style ANALYTICS fill:#FF9800
```

### Auto-Scaling Strategy
```mermaid
graph TB
    subgraph "Horizontal Pod Autoscaler (HPA)"
        METRICS[Metrics Collection]
        CPU[CPU Utilization > 70%]
        MEMORY[Memory Utilization > 80%]
        RPS[Requests/sec > 1000]
        LATENCY[P95 Latency > 500ms]
        QUEUE[Queue Depth > 100]
    end
    
    subgraph "Scaling Decision Engine"
        EVAL[Evaluate Metrics]
        DECISION{Scale<br/>Action?}
    end
    
    subgraph "Scaling Actions"
        SCALE_UP[Scale Up<br/>Add Pods]
        SCALE_DOWN[Scale Down<br/>Remove Pods]
        NO_ACTION[No Action]
    end
    
    subgraph "MCP Server Pods"
        MIN[Min: 3 Pods]
        MAX[Max: 20 Pods]
        CURRENT[Current: N Pods]
    end
    
    subgraph "AI Service Pods"
        AI_MIN[Min: 2 Pods]
        AI_MAX[Max: 10 Pods]
        AI_CURRENT[Current: M Pods]
    end
    
    METRICS --> CPU
    METRICS --> MEMORY
    METRICS --> RPS
    METRICS --> LATENCY
    METRICS --> QUEUE
    
    CPU --> EVAL
    MEMORY --> EVAL
    RPS --> EVAL
    LATENCY --> EVAL
    QUEUE --> EVAL
    
    EVAL --> DECISION
    
    DECISION -->|Threshold Exceeded| SCALE_UP
    DECISION -->|Under-utilized| SCALE_DOWN
    DECISION -->|Within Range| NO_ACTION
    
    SCALE_UP --> CURRENT
    SCALE_DOWN --> CURRENT
    
    SCALE_UP --> AI_CURRENT
    SCALE_DOWN --> AI_CURRENT
    
    MIN -.Min Limit.-> CURRENT
    MAX -.Max Limit.-> CURRENT
    
    AI_MIN -.Min Limit.-> AI_CURRENT
    AI_MAX -.Max Limit.-> AI_CURRENT
    
    style SCALE_UP fill:#4CAF50
    style SCALE_DOWN fill:#F44336
    style DECISION fill:#FF9800
```

### Database Schema (ERD)
```
erDiagram
    USERS ||--o{ CONVERSATIONS : has
    USERS ||--o{ TRAINING_JOBS : requests
    USERS ||--o{ ANALYTICS_EVENTS : generates
    
    USERS {
        varchar user_id PK
        varchar phone_number UK
        timestamp created_at
        timestamp last_active
        varchar model_version
        varchar subscription_tier
        boolean is_active
    }
    
    CONVERSATIONS {
        bigint conversation_id PK
        varchar user_id FK
        text message
        text response
        float confidence
        int processing_time_ms
        timestamp created_at
        varchar model_version
    }
    
    TRAINING_JOBS {
        varchar job_id PK
        varchar user_id FK
        varchar status
        int samples_processed
        timestamp started_at
        timestamp completed_at
        text error_message
        varchar s3_data_path
    }
    
    ANALYTICS_EVENTS {
        bigint event_id PK
        varchar user_id FK
        varchar event_type
        jsonb event_data
        timestamp created_at
    }
    
    API_KEYS {
        varchar key_id PK
        varchar hashed_key UK
        varchar client_name
        int rate_limit
        boolean is_active
        timestamp created_at
        timestamp expires_at
    }
```

### Infrastructure Deployment
```mermaid
graph TB
    subgraph "AWS Cloud"
        subgraph "Region: us-east-1"
            subgraph "Availability Zone 1"
                LB1[Load Balancer Node 1]
                K8S_WORKER1[K8s Worker Node 1<br/>MCP Pods]
                K8S_WORKER2[K8s Worker Node 2<br/>AI Pods]
                RDS_PRIMARY[(RDS Primary)]
                REDIS_MASTER1[Redis Master 1]
            end
            
            subgraph "Availability Zone 2"
                LB2[Load Balancer Node 2]
                K8S_WORKER3[K8s Worker Node 3<br/>MCP Pods]
                K8S_WORKER4[K8s Worker Node 4<br/>AI Pods]
                RDS_STANDBY[(RDS Standby)]
                REDIS_MASTER2[Redis Master 2]
            end
            
            subgraph "Availability Zone 3"
                K8S_WORKER5[K8s Worker Node 5<br/>Training Pods + GPU]
                REDIS_MASTER3[Redis Master 3]
            end
            
            subgraph "Shared Services"
                KAFKA_CLUSTER[Kafka Cluster<br/>3 Brokers Across AZs]
                S3_STORAGE[S3 Storage<br/>Multi-AZ]
                MONITORING[Monitoring Stack<br/>Prometheus + Grafana]
            end
        end
    end
    
    LB1 --> K8S_WORKER1
    LB1 --> K8S_WORKER3
    LB2 --> K8S_WORKER1
    LB2 --> K8S_WORKER3
    
    K8S_WORKER1 --> RDS_PRIMARY
    K8S_WORKER3 --> RDS_PRIMARY
    
    RDS_PRIMARY -.Replication.-> RDS_STANDBY
    
    K8S_WORKER1 --> REDIS_MASTER1
    K8S_WORKER3 --> REDIS_MASTER2
    K8S_WORKER2 --> REDIS_MASTER1
    K8S_WORKER4 --> REDIS_MASTER2
    
    REDIS_MASTER1 -.Replication.-> REDIS_MASTER2
    REDIS_MASTER2 -.Replication.-> REDIS_MASTER3
    
    K8S_WORKER1 --> KAFKA_CLUSTER
    K8S_WORKER3 --> KAFKA_CLUSTER
    K8S_WORKER5 --> KAFKA_CLUSTER
    
    K8S_WORKER2 --> S3_STORAGE
    K8S_WORKER4 --> S3_STORAGE
    K8S_WORKER5 --> S3_STORAGE
    
    K8S_WORKER1 --> MONITORING
    K8S_WORKER2 --> MONITORING
    K8S_WORKER5 --> MONITORING
    
    style LB1 fill:#FF9800
    style LB2 fill:#FF9800
    style RDS_PRIMARY fill:#9C27B0
    style RDS_STANDBY fill:#9C27B0
    style K8S_WORKER1 fill:#4CAF50
    style K8S_WORKER2 fill:#2196F3
    style K8S_WORKER3 fill:#4CAF50
    style K8S_WORKER4 fill:#2196F3
    style K8S_WORKER5 fill:#FF5722
```
### Network Architecture
```mermaid
graph TB
    subgraph "Public Internet"
        USERS[End Users]
        ADMIN[Admin Dashboard]
    end
    
    subgraph "Edge Layer - DMZ"
        WAF[Web Application Firewall<br/>Rate Limiting DDoS Protection]
        CDN[CloudFront CDN<br/>Static Assets]
    end
    
    subgraph "Public Subnet"
        ALB[Application Load Balancer<br/>SSL Termination Health Checks]
        NAT[NAT Gateway<br/>Outbound Internet Access]
    end
    
    subgraph "Private Subnet - Application Tier"
        MCP_CLUSTER[MCP Server Cluster<br/>10.0.1.0/24]
        AI_CLUSTER[AI Service Cluster<br/>10.0.2.0/24]
    end
    
    subgraph "Private Subnet - Data Tier"
        RDS_CLUSTER[PostgreSQL RDS<br/>10.0.10.0/24]
        REDIS_CLUSTER[Redis Cluster<br/>10.0.11.0/24]
        KAFKA_CLUSTER[Kafka Cluster<br/>10.0.12.0/24]
    end
    
    subgraph "Isolated Subnet - Training"
        GPU_NODES[GPU Training Nodes<br/>10.0.20.0/24]
    end
    
    subgraph "Management Subnet"
        BASTION[Bastion Host<br/>SSH Access Only]
        MONITORING[Monitoring Services<br/>10.0.30.0/24]
    end
    
    USERS --> WAF
    ADMIN --> WAF
    WAF --> CDN
    WAF --> ALB
    
    ALB --> MCP_CLUSTER
    MCP_CLUSTER --> AI_CLUSTER
    
    MCP_CLUSTER --> RDS_CLUSTER
    MCP_CLUSTER --> REDIS_CLUSTER
    MCP_CLUSTER --> KAFKA_CLUSTER
    
    AI_CLUSTER --> KAFKA_CLUSTER
    
    KAFKA_CLUSTER --> GPU_NODES
    GPU_NODES --> NAT
    
    MCP_CLUSTER --> MONITORING
    AI_CLUSTER --> MONITORING
    
    BASTION -.SSH Only.-> MCP_CLUSTER
    BASTION -.SSH Only.-> AI_CLUSTER
    
    style WAF fill:#F44336
    style ALB fill:#FF9800
    style MCP_CLUSTER fill:#4CAF50
    style AI_CLUSTER fill:#2196F3
    style RDS_CLUSTER fill:#9C27B0
    style GPU_NODES fill:#FF5722
```

### Security Architecture
```mermaid
graph TB
    subgraph "External Layer"
        CLIENT[Client Application]
    end
    
    subgraph "Security Perimeter"
        WAF[WAF Rules<br/>SQL Injection Prevention<br/>XSS Protection Rate Limiting]
        DDOS[DDoS Protection<br/>Shield Standard]
    end
    
    subgraph "Authentication Layer"
        API_AUTH[API Key Validation]
        JWT_AUTH[JWT Token Validation]
        OAUTH[OAuth 2.0<br/>WhatsApp Integration]
    end
    
    subgraph "Authorization Layer"
        RBAC[Role-Based Access Control<br/>User/Admin/System Roles]
        SCOPE[Scope Validation<br/>Resource Permissions]
    end
    
    subgraph "Application Security"
        INPUT_VAL[Input Validation<br/>Sanitization]
        RATE_LIMIT[Rate Limiting<br/>Per User/API Key]
        CSRF[CSRF Protection]
    end
    
    subgraph "Data Security"
        ENCRYPT_TRANSIT[TLS 1.3<br/>In-Transit Encryption]
        ENCRYPT_REST[AES-256<br/>At-Rest Encryption]
        DATA_MASK[Data Masking<br/>PII Protection]
    end
    
    subgraph "Network Security"
        SG[Security Groups<br/>Firewall Rules]
        NACL[Network ACLs<br/>Subnet Level Rules]
        VPN[VPN Gateway<br/>Admin Access]
    end
    
    subgraph "Monitoring & Audit"
        SIEM[SIEM System<br/>Security Events]
        AUDIT_LOG[Audit Logs<br/>All Access Tracked]
        ALERT[Security Alerts<br/>Anomaly Detection]
    end
    
    CLIENT --> WAF
    CLIENT --> DDOS
    WAF --> API_AUTH
    WAF --> JWT_AUTH
    
    API_AUTH --> RBAC
    JWT_AUTH --> RBAC
    OAUTH --> JWT_AUTH
    
    RBAC --> SCOPE
    SCOPE --> INPUT_VAL
    INPUT_VAL --> RATE_LIMIT
    RATE_LIMIT --> CSRF
    
    CSRF --> ENCRYPT_TRANSIT
    ENCRYPT_TRANSIT --> ENCRYPT_REST
    
    SG --> NACL
    NACL --> VPN
    
    API_AUTH --> AUDIT_LOG
    RBAC --> AUDIT_LOG
    ENCRYPT_TRANSIT --> AUDIT_LOG
    
    AUDIT_LOG --> SIEM
    SIEM --> ALERT
    
    style WAF fill:#F44336
    style API_AUTH fill:#FF9800
    style RBAC fill:#FF9800
    style ENCRYPT_TRANSIT fill:#4CAF50
    style ENCRYPT_REST fill:#4CAF50
    style SIEM fill:#9C27B0
```

### Monitoring & Observability
```mermaid
graph TB
    subgraph "Application Layer"
        MCP[MCP Server Pods]
        AI[AI Service Pods]
        TRAIN[Training Workers]
    end
    
    subgraph "Metrics Collection"
        PROM_AGENT[Prometheus Agent<br/>Scrape Metrics]
        PROM_SERVER[Prometheus Server<br/>TSDB Storage]
        PROM_ALERT[Alert Manager<br/>Alert Routing]
    end
    
    subgraph "Tracing"
        JAEGER_AGENT[Jaeger Agent<br/>Collect Spans]
        JAEGER_COLLECTOR[Jaeger Collector<br/>Process Traces]
        JAEGER_STORAGE[(Trace Storage<br/>Cassandra/ES)]
        JAEGER_UI[Jaeger UI<br/>Trace Visualization]
    end
    
    subgraph "Logging"
        FILEBEAT[Filebeat<br/>Log Shipper]
        LOGSTASH[Logstash<br/>Log Processing]
        ELASTIC[(Elasticsearch<br/>Log Storage)]
        KIBANA[Kibana<br/>Log Search & Viz]
    end
    
    subgraph "Visualization"
        GRAFANA[Grafana<br/>Dashboards]
        CUSTOM_DASH[Custom Dashboards<br/>Business Metrics]
    end
    
    subgraph "Alerting"
        SLACK[Slack Notifications]
        PAGERDUTY[PagerDuty<br/>On-Call Alerts]
        EMAIL[Email Alerts]
    end
    
    MCP --> PROM_AGENT
    AI --> PROM_AGENT
    TRAIN --> PROM_AGENT
    
    PROM_AGENT --> PROM_SERVER
    PROM_SERVER --> PROM_ALERT
    PROM_SERVER --> GRAFANA
    
    MCP --> JAEGER_AGENT
    AI --> JAEGER_AGENT
    JAEGER_AGENT --> JAEGER_COLLECTOR
    JAEGER_COLLECTOR --> JAEGER_STORAGE
    JAEGER_STORAGE --> JAEGER_UI
    
    MCP --> FILEBEAT
    AI --> FILEBEAT
    TRAIN --> FILEBEAT
    FILEBEAT --> LOGSTASH
    LOGSTASH --> ELASTIC
    ELASTIC --> KIBANA
    
    GRAFANA --> CUSTOM_DASH
    
    PROM_ALERT --> SLACK
    PROM_ALERT --> PAGERDUTY
    PROM_ALERT --> EMAIL
    
    style PROM_SERVER fill:#E65100
    style JAEGER_COLLECTOR fill:#00BCD4
    style ELASTIC fill:#00C853
    style GRAFANA fill:#FF6F00
    style PAGERDUTY fill:#F44336
```
