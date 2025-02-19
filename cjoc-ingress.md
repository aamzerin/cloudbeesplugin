
# 📜 CloudBees CJOC on EKS - Troubleshooting & ALB Configuration

## **User Query: Accessing CJOC UI via Ingress**

**User:**  
I have a CloudBees CJOC pod running in EKS, I also created an ingress for it and have an NGINX ingress controller. What do I need to do to access the CJOC UI from the browser?

---

## ✅ **1. Verify the Ingress Configuration**

First, ensure that your **Ingress resource** correctly routes traffic to the CJOC pod.

Run:
```bash
kubectl get ingress -n <namespace>
```
Check that it has an **External IP or hostname** assigned.

Example **Ingress YAML**:
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: cjoc-ingress
  namespace: <your-namespace>
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "false"
spec:
  ingressClassName: nginx
  rules:
  - host: cjoc.example.com  # Replace with your domain or AWS ALB hostname
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: cjoc-service
            port:
              number: 8080
```

**Things to check:**
- The host field (`cjoc.example.com`) matches the domain you'll use.
- The backend service points to the CJOC **Service** (check the name and port).

---

## ✅ **2. Check the CJOC Service**

Ensure the **CJOC service** exposes the correct port:

```bash
kubectl get svc -n <namespace>
```

Expected output:
```
NAME           TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)
cjoc-service   ClusterIP   10.100.200.10   <none>        8080/TCP
```

If the service is **NodePort** or **LoadBalancer**, adjust your Ingress accordingly.

---

## ✅ **3. Verify the NGINX Ingress Controller**

Your **NGINX Ingress Controller** should be running and managing the Ingress.

Check with:
```bash
kubectl get pods -n ingress-nginx
```

Look for a running **nginx-ingress-controller** pod.

Check logs for errors:
```bash
kubectl logs -n ingress-nginx <nginx-ingress-pod-name>
```

Ensure the controller has an external IP:
```bash
kubectl get svc -n ingress-nginx
```

Expected output:
```
NAME                                 TYPE           CLUSTER-IP      EXTERNAL-IP
nginx-ingress-controller             LoadBalancer  10.100.200.15   a1b2c3d4e5.us-east-1.elb.amazonaws.com
```

If **EXTERNAL-IP** is `<pending>`, AWS Load Balancer might not be provisioned. See troubleshooting steps below.

---

## **User:**  
**Nginx ingress controller external-ip pending**

---

## 🚨 **Troubleshooting: NGINX Ingress External IP Pending**

### ✅ **1. Check the NGINX Ingress Service**
Run:
```bash
kubectl get svc -n ingress-nginx
```

Expected output:
```
NAME                                 TYPE           CLUSTER-IP      EXTERNAL-IP   PORT(S)
nginx-ingress-controller             LoadBalancer  10.100.200.15   <pending>     80:31219/TCP, 443:31220/TCP
```

If `EXTERNAL-IP` is **pending**, proceed with troubleshooting.

### ✅ **2. Check AWS LoadBalancer Events**
Run:
```bash
kubectl describe svc nginx-ingress-controller -n ingress-nginx
```
Look for events like:
```
Warning  CreatingLoadBalancerFailed  ...
```
This indicates that AWS couldn't create the LoadBalancer.

### ✅ **3. Ensure Your Cluster is in a Public Subnet**
AWS Load Balancers need to be created in **public subnets**.

Find your subnets:
```bash
aws ec2 describe-subnets --filters "Name=vpc-id,Values=<your-vpc-id>"
```

Ensure at least one subnet has an **Internet Gateway** route:
```
Destination: 0.0.0.0/0
Target: Internet Gateway (igw-xxxxx)
```

### ✅ **4. Tag the Subnets for Load Balancer**
Check your subnets:
```bash
aws ec2 describe-subnets --filters "Name=vpc-id,Values=<your-vpc-id>" --query 'Subnets[*].[SubnetId,Tags]'
```

Ensure they have **these tags**:
```json
{
  "Key": "kubernetes.io/role/elb",
  "Value": "1"
}
{
  "Key": "kubernetes.io/cluster/<your-cluster-name>",
  "Value": "owned"
}
```

If missing, add them:
```bash
aws ec2 create-tags --resources <subnet-id> --tags Key=kubernetes.io/role/elb,Value=1
aws ec2 create-tags --resources <subnet-id> --tags Key=kubernetes.io/cluster/<your-cluster-name>,Value=owned
```

### ✅ **5. Restart the NGINX Ingress Controller**
```bash
kubectl delete pod -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx
```

After a few minutes, check again:
```bash
kubectl get svc -n ingress-nginx
```

---

## ✅ **6. Use AWS ALB Instead of NGINX**
If NGINX doesn't work, you can use the **AWS Load Balancer Controller**.

1. Install ALB Controller:
   ```bash
   helm install aws-load-balancer-controller eks/aws-load-balancer-controller      -n kube-system      --set clusterName=<your-cluster-name>      --set serviceAccount.create=false      --set serviceAccount.name=aws-load-balancer-controller
   ```

2. Create an ALB-based Ingress:
   ```yaml
   apiVersion: networking.k8s.io/v1
   kind: Ingress
   metadata:
     name: cjoc-ingress
     namespace: <your-namespace>
     annotations:
       alb.ingress.kubernetes.io/scheme: internet-facing
   spec:
     ingressClassName: alb
     rules:
     - host: cjoc.example.com
       http:
         paths:
         - path: /
           pathType: Prefix
           backend:
             service:
               name: cjoc-service
               port:
                 number: 8080
   ```

3. Apply it:
   ```bash
   kubectl apply -f cjoc-ingress.yaml
   ```

---

## ✅ **Final Steps**

Check again:
```bash
kubectl get svc -n ingress-nginx
```
Now, you should see a **valid external IP** or AWS ALB hostname!

---

## **User:**  
**Issue resolved! Thanks!**

**Response:**  
Super nouvelle ! 🎉 Ravi d'avoir pu t'aider. Si jamais tu rencontres d'autres soucis sur EKS ou tout autre sujet DevOps, n'hésite pas. Bonne continuation et bon déploiement ! 🚀😃
