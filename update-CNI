
# Débogage de l'erreur AWS-CNI dans Kubernetes

## Description de l'erreur

L'erreur observée indique un problème avec la configuration réseau dans un environnement Kubernetes utilisant le plugin CNI d'AWS. Voici les points clés et les solutions proposées.

---

## Points clés de l'erreur
1. **"Failed to create pod sandbox"** : Kubernetes ne parvient pas à initialiser le sandbox réseau pour le pod.
2. **"plugin type=\"aws-cni\""** : Le problème est lié au plugin réseau AWS CNI.
3. **"Error received from AddNetwork gRPC call"** : Une erreur a été retournée lors de l'appel RPC gRPC pour ajouter le réseau.
4. **"connection error: transport: Error while dialing: dial tcp 127.0.0.1:50051: connect: connection refused"** : Le processus qui écoute sur `127.0.0.1:50051` ne fonctionne pas ou n'est pas accessible.

---

## Étapes générales pour résoudre le problème

### 1. Vérifiez l'état du daemon AWS CNI
- Vérifiez si le daemon `aws-k8s-agent` ou `aws-node` fonctionne correctement :
  ```bash
  kubectl get pods -n kube-system
  ```
  Assurez-vous que le pod `aws-node` est en statut "Running". Si ce n'est pas le cas, consultez ses logs :
  ```bash
  kubectl logs -n kube-system <aws-node-pod-name>
  ```

### 2. Redémarrez le pod `aws-node`
Si le pod est bloqué ou affiche des erreurs :
```bash
kubectl delete pod -n kube-system <aws-node-pod-name>
```

### 3. Vérifiez la connectivité réseau locale
- Assurez-vous que le service gRPC fonctionne sur le port `50051` sur le nœud :
  ```bash
  netstat -tulnp | grep 50051
  ```
  Si le service ne tourne pas, redémarrez l'agent AWS CNI.

### 4. Mettre à jour AWS CNI
- Vérifiez si vous utilisez une version obsolète du plugin AWS CNI :
  ```bash
  kubectl describe daemonset aws-node -n kube-system | grep Image
  ```
- Si nécessaire, mettez-le à jour :
  ```bash
  kubectl apply -f https://raw.githubusercontent.com/aws/amazon-vpc-cni-k8s/master/config/v1.12/aws-k8s-cni.yaml
  ```

### 5. Vérifiez les IAM Roles
Assurez-vous que le rôle IAM attaché aux nœuds a les permissions nécessaires pour gérer le réseau.

### 6. Inspectez les logs système
- Si les étapes ci-dessus ne résolvent pas le problème, inspectez les logs système de Docker ou containerd pour des indices :
  ```bash
  sudo journalctl -u docker
  sudo journalctl -u containerd
  ```

### 7. Redémarrez le nœud
Si le problème persiste, redémarrez le nœud concerné.

### 8. Vérifiez les quotas de VPC
- Assurez-vous que vous n'avez pas atteint les quotas de sous-réseaux, adresses IP ou interfaces réseau dans le VPC.

---

## Étapes spécifiques selon la version

### Contexte : Image `aws-cni-init:v1.19.0`
L'image utilisée pour `aws-cni-init` est **v1.19.0**. Voici les actions à entreprendre :

### 1. Vérifiez la compatibilité des versions
- Assurez-vous que la version de **`aws-cni-init` (v1.19.0)** est compatible avec la version d'EKS que vous utilisez. 
- Pour lister les versions des composants :
  ```bash
  kubectl get ds -n kube-system aws-node -o yaml | grep image
  ```

### 2. Mettre à jour AWS CNI
Si vous utilisez une version obsolète, il est recommandé de mettre à jour le plugin CNI. Pour cela :
```bash
kubectl apply -f https://raw.githubusercontent.com/aws/amazon-vpc-cni-k8s/release-<version>/config/v1.19/aws-k8s-cni.yaml
```
Remplacez `<version>` par la dernière version stable.

### 3. Vérifiez les permissions IAM
Le rôle IAM du nœud doit inclure les permissions suivantes :
- `ec2:AttachNetworkInterface`
- `ec2:CreateNetworkInterface`
- `ec2:DeleteNetworkInterface`
- `ec2:DescribeInstances`
- `ec2:DescribeNetworkInterfaces`
- `ec2:DetachNetworkInterface`

### 4. Redémarrez les pods `aws-node`
Si les étapes ci-dessus ne corrigent pas le problème immédiatement, essayez de redémarrer les pods du daemonset `aws-node` :
```bash
kubectl rollout restart daemonset/aws-node -n kube-system
```

### 5. Vérifiez la disponibilité du port 50051
- Connectez-vous au nœud concerné et vérifiez si le port **50051** est ouvert :
  ```bash
  sudo netstat -tulnp | grep 50051
  ```
- Si le service n'écoute pas sur ce port, redémarrez l'agent AWS CNI :
  ```bash
  sudo systemctl restart aws-k8s-agent
  ```

### 6. Inspectez les logs du pod `aws-node`
- Récupérez les logs pour obtenir plus de détails sur la cause du problème :
  ```bash
  kubectl logs -n kube-system <aws-node-pod-name>
  ```

### 7. Assurez-vous que les quotas AWS ne sont pas atteints
- Si votre cluster gère un grand nombre de pods ou de sous-réseaux, vérifiez que vous n'avez pas atteint les limites AWS, comme le nombre maximal d'interfaces réseau ou d'adresses IP dans le VPC.

### 8. Déboguez avec l'outil officiel d'AWS
AWS propose un outil pour diagnostiquer les problèmes CNI :
```bash
kubectl apply -f https://raw.githubusercontent.com/aws/amazon-vpc-cni-k8s/master/scripts/troubleshooting/support-bundle.yaml
```

---

## Conclusion
Ces étapes devraient résoudre votre problème ou permettre d'identifier précisément la source. Si le problème persiste, consultez les logs obtenus ou contactez le support AWS.

