
# 📘 kube2iam vs IAM Roles for Service Accounts (IRSA) in Amazon EKS

*Generated on 2025-04-25*

---

## 🔍 Overview

This document presents a thorough comparison between `kube2iam` and `IAM Roles for Service Accounts (IRSA)` in Amazon EKS, including configuration examples, potential conflicts, and a strategic conclusion advocating the migration to IRSA.

---

## ✅ Comparison Table

| Feature | kube2iam | IRSA |
|--------|-----------|------|
| **Pod-to-role binding** | Pod annotations | Service account binding |
| **Credential source** | EC2 metadata + iptables | OIDC Web Identity |
| **Security** | Intercepts metadata (bypass risk) | Secure, least-privilege, token-based |
| **Auditability** | Limited | AWS CloudTrail integration |
| **Scalability** | iptables limits at scale | Fully scalable |
| **AWS support** | No | Official and recommended |
| **Fargate support** | ❌ Not supported | ✅ Fully supported |

---

## 🔧 kube2iam Setup Example

1. **Install kube2iam** using Helm.
2. **Create IAM Role** with trust policy to EC2 nodes.
3. **Deploy pod with annotation**:

```yaml
annotations:
  iam.amazonaws.com/role: arn:aws:iam::<ACCOUNT_ID>:role/kube2iam-demo-role
```

---

## 🔧 IRSA Setup Example

1. **Associate OIDC provider** to EKS.
2. **Create IAM Role** with trust policy to SA identity.
3. **Annotate service account**:

```bash
kubectl annotate serviceaccount irsa-sa   eks.amazonaws.com/role-arn=arn:aws:iam::<ACCOUNT_ID>:role/irsa-demo-role
```

4. **Deploy pod using that SA**.

---

## 🧱 Running Both kube2iam and IRSA in Parallel

Yes, it's possible but:

- Requires strict **network isolation**.
- Avoid overlapping **credential resolution paths**.
- Use **NetworkPolicies** to block IRSA pods from 169.254.169.254.
- Do **not define both** IRSA SA and kube2iam annotation on the same pod.

---

## 🔥 Common Conflicts and How to Avoid Them

| Conflict | Cause | Mitigation |
|---------|-------|------------|
| **IRSA pod falls back to kube2iam** | IRSA misconfigured, metadata accessible | Block metadata endpoint with NetworkPolicy |
| **kube2iam intercepts IRSA pod** | Wrong iptables setup | Use pod labels to isolate kube2iam routing |
| **Role assumption too broad** | Bad trust policy | Scope to `system:serviceaccount:<ns>:<name>` |
| **kube2iam fallback role used** | `--default-role` enabled | Set to empty or remove |

---

## 🧾 Final Recommendation to Management

We recommend a full **deprecation of kube2iam** in favor of **IRSA**. Key reasons:

- 🔐 **Security**: IRSA removes metadata endpoint exposure.
- 🧰 **Simplicity**: No DaemonSets, iptables, or privileged pods.
- 📈 **Scalability**: IRSA is cloud-native and scales cleanly.
- 💀 **kube2iam is legacy**: Not AWS-supported, not future-proof.
- ☁️ **IRSA is required for Fargate and newer AWS services**.

### ✅ Immediate Action Plan:
- Freeze kube2iam usage.
- Begin phased migration to IRSA.
- Adopt service account-per-role architecture.

---

For questions, migration support, or architecture templates, reach out to your platform team or DevOps engineering.

