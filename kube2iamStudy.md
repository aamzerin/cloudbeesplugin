
# 📘 kube2iam vs IAM Roles for Service Accounts (IRSA) in Amazon EKS

*Thorough study, configuration examples, conflict scenarios, and strategic recommendation.*

---

## 🔍 Comparison Overview

| Feature | kube2iam | IRSA |
|--------|-----------|------|
| **Pod-to-role binding** | Pod annotations | Service account binding |
| **Credential source** | EC2 metadata + iptables | OIDC Web Identity |
| **Security** | Intercepts metadata (bypass risk) | Secure, least-privilege, token-based |
| **Auditability** | Limited | AWS CloudTrail integration |
| **Scalability** | iptables limits at scale | Fully scalable |
| **AWS support** | No | Official and recommended |
| **Fargate support** | ❌ Not supported | ✅ Fully supported |
| **Granularity** | Per pod (IP-based) | Per service account |
| **Complexity** | Higher (iptables, DaemonSet) | Lower (native to Kubernetes) |
| **Future-proof** | ❌ Legacy | ✅ Recommended by AWS |

---

## ⚙️ Can Both Be Used at the Same Time?

✅ **Yes**, but not recommended long-term.

### Considerations:
- AWS SDKs will **prefer IRSA** if web identity is configured.
- If IRSA fails, SDK may **fall back to kube2iam**.
- Must **isolate kube2iam** access using iptables or NetworkPolicies.
- Maintain strict trust policies to avoid privilege escalation.

---

## ✅ kube2iam Configuration Example

### 1. Install via Helm

```bash
helm repo add kube2iam https://jtblin.github.io/kube2iam/
helm install kube2iam kube2iam/kube2iam   --namespace kube-system   --set host.iptables=true   --set host.interface=eni+
```

### 2. Create IAM Role for kube2iam

**Trust policy** example:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": { "AWS": "arn:aws:iam::<ACCOUNT_ID>:role/nodes-role" },
      "Action": "sts:AssumeRole"
    }
  ]
}
```

### 3. Annotate Pod with IAM Role

```yaml
annotations:
  iam.amazonaws.com/role: arn:aws:iam::<ACCOUNT_ID>:role/kube2iam-demo-role
```

---

## ✅ IRSA Configuration Example

### 1. Associate OIDC Provider

```bash
eksctl utils associate-iam-oidc-provider   --region <region>   --cluster <cluster-name>   --approve
```

### 2. Create IAM Role with Trust for SA

**Trust policy:**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::<ACCOUNT_ID>:oidc-provider/<OIDC_PROVIDER>"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "<OIDC_PROVIDER>:sub": "system:serviceaccount:default:irsa-sa"
        }
      }
    }
  ]
}
```

### 3. Annotate Service Account

```bash
kubectl annotate serviceaccount irsa-sa   eks.amazonaws.com/role-arn=arn:aws:iam::<ACCOUNT_ID>:role/irsa-demo-role
```

### 4. Attach SA to Pod

```yaml
spec:
  serviceAccountName: irsa-sa
```

---

## 🔥 Conflict Scenarios and Mitigation

### Conflict 1: IRSA Pod Falls Back to kube2iam

**Risk**: IRSA pod silently uses kube2iam.

**Fix**:
- Block 169.254.169.254 via NetworkPolicy or iptables.

---

### Conflict 2: kube2iam Routes IRSA Pods via iptables

**Risk**: kube2iam proxies metadata requests from IRSA pods.

**Fix**:
- Use labels and `iptables-additional-routes` to isolate kube2iam routing.
- Disable `--default-role`.

---

### Conflict 3: Broad Trust Policy for IRSA

**Risk**: Multiple SAs can assume one role.

**Fix**:
- Scope trust to exact SA (`system:serviceaccount:<ns>:<sa>`).

---

### Conflict 4: Mixed Setup Creates Debugging Headaches

**Fix**:
- Use `aws sts get-caller-identity` inside pod to verify identity.
- Document roles and bindings clearly.

---

## 🧰 Isolation Strategy Table

| Layer | kube2iam Pods | IRSA Pods |
|-------|----------------|------------|
| Namespace | `legacy-ns` | `secure-ns` |
| SA | No IRSA annotation | IRSA annotated SA |
| Metadata Access | Allowed | Denied (via NetworkPolicy) |
| IAM Role Trust | EC2 Role-based | OIDC SA-based |
| Policy Granularity | Shared role | Per SA role |

---

## 🧾 Conclusion to Management and Users

### ❌ Why We Must Avoid kube2iam

1. **Security**:
   - Intercepts EC2 metadata; vulnerable if misconfigured.
   - Privileged DaemonSet, hard to audit.

2. **Complexity**:
   - Requires iptables, pod annotations, network controls.
   - Breaks principle of least privilege.

3. **Scalability**:
   - Doesn't scale well in large clusters.
   - IRSA offers per-SA roles, no proxy overhead.

4. **Supportability**:
   - kube2iam is community-maintained, not officially supported by AWS.
   - IRSA is AWS-native and required for Fargate.

5. **Future-proofing**:
   - kube2iam is incompatible with AWS Fargate and evolving AWS architectures.
   - IRSA is built for cloud-native, secure identity-based access.

---

## ✅ Final Recommendation

**Deprecate kube2iam usage immediately**:

- Freeze its use for new workloads.
- Begin migration of existing workloads to IRSA.
- Ensure isolation to avoid mixed-use conflicts during the transition.

This migration aligns with best practices, hardens security, simplifies operations, and enables EKS scalability.

---
