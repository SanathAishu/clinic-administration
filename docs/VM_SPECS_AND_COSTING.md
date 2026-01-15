# VM Specifications and Cost Analysis for Clinic Management System

## Table of Contents
1. [Resource Requirements Analysis](#resource-requirements-analysis)
2. [GCP VM Specifications](#gcp-vm-specifications)
3. [Cost Estimates (GCP asia-south1 Mumbai)](#cost-estimates-gcp-asia-south1-mumbai)
4. [Cost Optimization Strategies](#cost-optimization-strategies)
5. [Scaling Considerations](#scaling-considerations)

---

## Resource Requirements Analysis

### Current Docker Stack Components

| Service | RAM Usage (Est.) | CPU Usage (Est.) | Disk I/O |
|---------|------------------|------------------|----------|
| **PostgreSQL 16** | 2-4 GB | 1-2 cores | High |
| **Redis 7** | 512 MB - 1 GB | 0.5 cores | Medium |
| **MinIO** | 1-2 GB | 0.5-1 cores | High |
| **RabbitMQ** | 512 MB - 1 GB | 0.5 cores | Medium |
| **Spring Boot Backend** | 2-4 GB | 2-3 cores | Medium |
| **React Frontend (Nginx)** | 256 MB | 0.2 cores | Low |
| **Prometheus** | 1-2 GB | 0.5 cores | Medium |
| **Grafana** | 512 MB | 0.2 cores | Low |
| **Elasticsearch** | 2-4 GB | 1-2 cores | High |
| **Logstash** | 1-2 GB | 0.5-1 cores | Medium |
| **Kibana** | 512 MB | 0.2 cores | Low |
| **Nginx (Reverse Proxy)** | 128 MB | 0.1 cores | Low |
| **System Overhead** | 2 GB | 0.5 cores | - |
| **Total (Minimum)** | **13.5 GB** | **7.7 cores** | - |
| **Total (Comfortable)** | **23 GB** | **12 cores** | - |

### Disk Space Requirements

| Component | Storage Requirement |
|-----------|---------------------|
| Operating System (Ubuntu 22.04) | 10 GB |
| Docker Images | 8-10 GB |
| PostgreSQL Data | 20-50 GB (grows over time) |
| MinIO Documents | 50-100 GB (grows over time) |
| Elasticsearch Logs (30 days) | 10-20 GB |
| Prometheus Metrics (30 days) | 5-10 GB |
| Backup Storage | 50-100 GB |
| System Swap | 8 GB |
| Buffer/Free Space (20%) | 40 GB |
| **Total Minimum** | **201 GB** |
| **Recommended** | **350-500 GB** |

---

## GCP VM Specifications

### Option 1: Development/Testing Environment (Small Scale)

**Machine Type: e2-standard-4**
- **vCPUs**: 4 vCPUs (shared core)
- **Memory**: 16 GB
- **Boot Disk**: 200 GB SSD (pd-ssd)
- **Region**: asia-south1 (Mumbai)
- **Use Case**: Development, testing, small clinic (1-2 users)
- **Limitations**:
  - No ELK stack (use Cloud Logging instead)
  - Reduced Prometheus retention (7 days)
  - Single clinic tenant

**Estimated Monthly Cost**: ₹8,000 - ₹10,000

### Option 2: Production Environment (Recommended - Medium Scale)

**Machine Type: n2-standard-8**
- **vCPUs**: 8 vCPUs (dedicated)
- **Memory**: 32 GB
- **Boot Disk**: 500 GB SSD (pd-ssd)
- **Region**: asia-south1 (Mumbai)
- **Use Case**: Production deployment, 3-5 concurrent clinics, 20-30 active users
- **Features**:
  - Full stack with ELK
  - 30-day log retention
  - Full monitoring suite
  - Automated backups

**Estimated Monthly Cost**: ₹18,000 - ₹22,000

### Option 3: Production Environment (Optimal - Large Scale)

**Machine Type: n2-highmem-8**
- **vCPUs**: 8 vCPUs (dedicated)
- **Memory**: 64 GB
- **Boot Disk**: 1 TB SSD (pd-ssd)
- **Additional Disk**: 500 GB pd-standard for backups
- **Region**: asia-south1 (Mumbai)
- **Use Case**: Large deployment, 10+ concurrent clinics, 50+ active users
- **Features**:
  - Full stack with ELK
  - 90-day log retention
  - Full monitoring suite
  - High-availability ready
  - Extensive caching

**Estimated Monthly Cost**: ₹32,000 - ₹38,000

### Option 4: Cost-Optimized Production (Budget)

**Machine Type: e2-highmem-4**
- **vCPUs**: 4 vCPUs (shared core)
- **Memory**: 32 GB
- **Boot Disk**: 350 GB SSD (pd-balanced)
- **Region**: asia-south1 (Mumbai)
- **Use Case**: Production deployment, 2-3 concurrent clinics, 15-20 active users
- **Features**:
  - Simplified logging (no full ELK, use Cloud Logging)
  - 15-day Prometheus retention
  - Essential monitoring only
  - Automated backups

**Estimated Monthly Cost**: ₹12,000 - ₹15,000

---

## Cost Estimates (GCP asia-south1 Mumbai)

### Detailed Breakdown - Option 2 (Recommended)

Based on [GCP Pricing Calculator](https://cloud.google.com/products/calculator) and [CloudPrice.net](https://cloudprice.net/gcp/compute/instances/n2-standard-8):

#### Compute Instance (n2-standard-8)

| Item | Specification | Monthly Cost (Approx.) |
|------|---------------|------------------------|
| **VM Instance** | n2-standard-8 (8 vCPU, 32GB RAM) | ₹12,500 |
| **Sustained Use Discount** | -25% (auto-applied after 25% usage) | -₹3,125 |
| **Net VM Cost** | | **₹9,375** |

#### Storage

| Item | Specification | Monthly Cost (Approx.) |
|------|---------------|------------------------|
| **Boot Disk (SSD)** | 500 GB pd-ssd | ₹6,500 |
| **Backup Disk** | 200 GB pd-standard | ₹650 |
| **Snapshots** | 100 GB (incremental) | ₹325 |
| **Total Storage** | | **₹7,475** |

#### Network

| Item | Specification | Monthly Cost (Approx.) |
|------|---------------|------------------------|
| **Static IP** | 1 Reserved IP | ₹300 |
| **Egress Traffic** | ~100 GB/month (within India) | ₹500 |
| **Total Network** | | **₹800** |

#### Additional Services (Optional)

| Item | Specification | Monthly Cost (Approx.) |
|------|---------------|------------------------|
| **Cloud Load Balancer** | If using (optional) | ₹2,000 |
| **Cloud Armor** | Basic DDoS protection | ₹1,500 |
| **Cloud Monitoring** | Enhanced monitoring | ₹500 |
| **Total Optional** | | **₹4,000** |

### Total Monthly Cost Summary

| Configuration | Without Optional Services | With Optional Services |
|---------------|---------------------------|------------------------|
| **Dev/Test (e2-standard-4)** | ₹8,500 | ₹12,500 |
| **Production Budget (e2-highmem-4)** | ₹13,200 | ₹17,200 |
| **Production Recommended (n2-standard-8)** | ₹17,650 | ₹21,650 |
| **Production Optimal (n2-highmem-8)** | ₹26,800 | ₹30,800 |

### Annual Cost Summary (with Committed Use Discounts)

GCP offers committed use discounts for 1-year or 3-year commitments:
- **1-year commitment**: 37% discount
- **3-year commitment**: 55% discount

| Configuration | Monthly (On-Demand) | Monthly (1-year CUD) | Annual Savings | Monthly (3-year CUD) | Annual Savings |
|---------------|---------------------|---------------------|----------------|---------------------|----------------|
| **Production Recommended** | ₹17,650 | ₹11,120 | ₹78,360 | ₹7,943 | ₹1,16,484 |
| **Production Optimal** | ₹26,800 | ₹16,884 | ₹1,18,992 | ₹12,060 | ₹1,76,880 |

---

## Cost Optimization Strategies

### 1. Use Committed Use Discounts (CUD)

**Recommendation**: For production deployments, commit to 1-year or 3-year contracts.

**Savings**:
- 1-year: 37% discount = **₹78,360/year** (for n2-standard-8)
- 3-year: 55% discount = **₹1,16,484/year** (for n2-standard-8)

### 2. Use Spot/Preemptible VMs for Development

**Recommendation**: Use preemptible VMs for dev/test environments.

**Savings**:
- Preemptible instances: Up to 80% cheaper
- Dev/Test monthly cost: ₹8,500 → **₹1,700/month**

**Limitations**:
- Can be terminated with 30 seconds notice
- Not suitable for production

### 3. Optimize Disk Usage

| Strategy | Savings |
|----------|---------|
| Use pd-balanced instead of pd-ssd for non-critical data | 40% on storage costs |
| Enable automatic snapshots with lifecycle policies | Retain only 7 days of snapshots |
| Use pd-standard for backup storage | 65% cheaper than pd-ssd |

**Potential Savings**: ₹2,000-3,000/month

### 4. Right-Size Resources

**Start with**: e2-highmem-4 (₹13,200/month)
**Monitor for 1 month** using Prometheus/Grafana
**Scale up to**: n2-standard-8 if needed

**Potential Savings**: ₹4,500/month initially

### 5. Simplify Monitoring Stack

For small deployments, replace ELK stack with:
- **Cloud Logging** (pay-per-use, ~₹500-1,000/month for 10GB logs)
- **Cloud Monitoring** (built-in, minimal cost)

**Savings**:
- Reduce RAM requirement by 6 GB
- Reduce CPU requirement by 2 cores
- **Save ₹3,000-4,000/month** on VM costs

### 6. Use Regional Persistent Disks

Instead of zonal persistent disks, use regional disks for high availability.

**Trade-off**:
- Cost: +100% for storage
- Benefit: Built-in replication across zones
- **Recommendation**: Use for production databases only

### 7. Network Optimization

| Strategy | Savings |
|----------|---------|
| Use Cloud CDN for static assets (frontend) | Reduce egress by 60-70% |
| Compress responses (gzip/brotli) | Reduce bandwidth by 40-50% |
| Cache aggressively with Redis | Reduce database load |

**Potential Savings**: ₹300-500/month on network costs

### 8. Scheduled Scaling

For dev/test environments:
- **Auto-shutdown** nights/weekends
- **Schedule**: Mon-Fri 9 AM - 6 PM

**Savings**:
- Run 45 hours/week instead of 168 hours/week
- Save 73% on dev/test costs
- **₹6,205/month** for dev environment

---

## Scaling Considerations

### Vertical Scaling Path

```
Development:
e2-standard-4 (4 vCPU, 16 GB) → ₹8,500/month
↓
Small Production:
e2-highmem-4 (4 vCPU, 32 GB) → ₹13,200/month
↓
Medium Production:
n2-standard-8 (8 vCPU, 32 GB) → ₹17,650/month
↓
Large Production:
n2-highmem-8 (8 vCPU, 64 GB) → ₹26,800/month
↓
Enterprise:
n2-highmem-16 (16 vCPU, 128 GB) → ₹48,000/month
```

### Horizontal Scaling (Multi-VM Architecture)

For enterprise deployments (50+ clinics, 200+ users):

```
Load Balancer (Cloud Load Balancer)
│
├─ Frontend Tier (2x e2-medium) → ₹3,400/month
│  └─ Nginx + React static files
│
├─ Application Tier (3x n2-standard-4) → ₹21,000/month
│  └─ Spring Boot instances (auto-scaling)
│
├─ Data Tier (1x n2-highmem-8) → ₹26,800/month
│  ├─ PostgreSQL (primary)
│  ├─ Redis
│  └─ MinIO
│
└─ Monitoring Tier (1x e2-standard-2) → ₹4,200/month
   ├─ Prometheus
   ├─ Grafana
   └─ ELK Stack

Total: ₹55,400/month (supports 200+ concurrent users)
```

### Hybrid Cloud Strategy (Cost Reduction)

**Scenario**: Mix GCP with other providers

```
GCP (asia-south1):
- Application + Database (n2-standard-8) → ₹17,650/month

On-Premise/Colocation:
- File Storage (NAS with 2TB) → ₹5,000/month
- Backup Storage → ₹3,000/month

Total: ₹25,650/month
Savings: ₹2,000/month vs pure GCP
```

### Database Scaling Options

| Option | When to Use | Cost Impact |
|--------|-------------|-------------|
| **Single VM with PostgreSQL** | < 5 clinics, < 1TB data | Included in VM cost |
| **Cloud SQL PostgreSQL** | 5-20 clinics, managed service preferred | +₹8,000-15,000/month |
| **Cloud SQL HA** | Production, high availability required | +₹20,000-30,000/month |
| **Cloud Spanner** | Multi-region, global scale | +₹50,000+/month |

---

## Recommended Configuration by Use Case

### 1. Single Clinic / Pilot (1-5 users)

**Configuration**: e2-standard-4
- **Compute**: ₹4,875/month (with CUD 1-year)
- **Storage**: 200 GB SSD → ₹2,600/month
- **Network**: ₹500/month
- **Total**: **₹7,975/month** or **₹95,700/year**

### 2. Small Practice (2-3 clinics, 10-20 users)

**Configuration**: e2-highmem-4
- **Compute**: ₹8,316/month (with CUD 1-year)
- **Storage**: 350 GB SSD → ₹4,550/month
- **Network**: ₹700/month
- **Total**: **₹13,566/month** or **₹1,62,792/year**

### 3. Medium Practice (5-10 clinics, 30-50 users) - RECOMMENDED

**Configuration**: n2-standard-8
- **Compute**: ₹11,120/month (with CUD 1-year)
- **Storage**: 500 GB SSD + 200 GB backup → ₹7,475/month
- **Network**: ₹1,000/month
- **Monitoring**: Cloud Logging/Monitoring → ₹800/month
- **Total**: **₹20,395/month** or **₹2,44,740/year**

**This is the recommended production configuration.**

### 4. Large Practice (10+ clinics, 50-100 users)

**Configuration**: n2-highmem-8
- **Compute**: ₹16,884/month (with CUD 1-year)
- **Storage**: 1 TB SSD + 500 GB backup → ₹15,950/month
- **Network**: ₹1,500/month
- **Cloud Armor**: ₹1,500/month
- **Monitoring**: ₹1,000/month
- **Total**: **₹36,834/month** or **₹4,42,008/year**

### 5. Enterprise (Multi-tenant SaaS, 100+ clinics)

**Configuration**: Multi-VM setup (horizontal scaling)
- **Application Tier**: 3x n2-standard-4 → ₹21,000/month
- **Database Tier**: Cloud SQL HA → ₹25,000/month
- **Load Balancer**: ₹2,500/month
- **Cloud CDN**: ₹1,500/month
- **Cloud Armor**: ₹2,000/month
- **Total**: **₹52,000/month** or **₹6,24,000/year**

---

## Summary & Recommendations

### For Your Development Phase

**Start with**: **e2-highmem-4** (4 vCPU, 32 GB RAM, 350 GB SSD)
- **Cost**: ₹13,200/month (on-demand)
- **Reasoning**:
  - Sufficient RAM for full stack
  - Shared cores acceptable for development
  - Can run all containers comfortably
  - Easy to scale up when needed

### For Production Deployment

**Recommended**: **n2-standard-8** (8 vCPU, 32 GB RAM, 500 GB SSD)
- **Cost**: ₹20,395/month (with 1-year CUD and optimizations)
- **Annual Cost**: ₹2,44,740/year
- **Reasoning**:
  - Dedicated cores for reliable performance
  - Sufficient resources for 5-10 clinics
  - Room for growth
  - Production-grade stability
  - Cost-effective with committed use discounts

### Cost Reduction Checklist

- [ ] **Commit to 1-year CUD** → Save 37% (₹78,360/year)
- [ ] **Use pd-balanced for non-critical storage** → Save ₹2,400/year
- [ ] **Replace ELK with Cloud Logging** (for small deployments) → Save ₹36,000/year
- [ ] **Enable auto-scaling for dev/test** → Save ₹74,460/year
- [ ] **Compress network traffic** → Save ₹3,600/year
- [ ] **Use Cloud CDN for static assets** → Save ₹4,800/year

**Total Potential Savings**: ₹1,99,620/year (45% reduction)

---

## References

- [GCP Pricing Calculator](https://cloud.google.com/products/calculator)
- [GCP Compute Engine Pricing](https://cloud.google.com/compute/vm-instance-pricing)
- [CloudPrice GCP Comparison](https://cloudprice.net/gcp/compute)
- [GCP Committed Use Discounts](https://cloud.google.com/compute/docs/instances/committed-use-discounts)
- [GCP Instance Comparison](https://gcpinstances.doit.com/)

---

**Document Version**: 1.0
**Last Updated**: 2026-01-15
**Exchange Rate**: ₹1 = ~$0.012 (for reference)
