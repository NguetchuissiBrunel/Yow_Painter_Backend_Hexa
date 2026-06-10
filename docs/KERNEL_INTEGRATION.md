# Intégration YowPainter ↔ Kernel RT-Comops

Ce document décrit l'activation du backend YowPainter en **mode consommateur hexagonal** du kernel RT-Comops.

## Prérequis kernel

1. Déployer le kernel RT-Comops (`D:\KSM_Kernel_Layer` ou stack Docker).
2. Créer une **ClientApplication** nommée `yowpainter-backend` dans le kernel.
3. Récupérer : `client-id`, `api-key`, `tenant-id` (UUID du tenant kernel).
4. S'assurer que le plan commercial `COMMERCE` (ou plan custom) est disponible.

## Variables d'environnement

```bash
KSM_KERNEL_BASE_URL=http://localhost:8080
KSM_KERNEL_CLIENT_ID=yowpainter-backend
KSM_KERNEL_API_KEY=<secret>
KSM_KERNEL_TENANT_ID=<uuid-tenant-kernel>
KSM_KERNEL_DEFAULT_PLAN_CODE=COMMERCE
KSM_KERNEL_DEFAULT_CURRENCY=XAF

# JWT RS256 via JWKS kernel (optionnel si base-url correcte)
KSM_KERNEL_JWK_SET_URI=http://localhost:8080/.well-known/jwks.json

# Kafka — synchronisation événements métier (optionnel)
KSM_KERNEL_KAFKA_ENABLED=true
KSM_KERNEL_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KSM_KERNEL_KAFKA_TOPIC=iwm.events.business
KSM_KERNEL_KAFKA_GROUP_ID=yowpainter-backend

# Migration one-shot organizationId sur données existantes
KSM_KERNEL_LEGACY_MIGRATION_ENABLED=true
```

Le backend YowPainter est **toujours** un consommateur du kernel (auth, org, commerce, fichiers, notifications). Le frontend **ne doit jamais** appeler le kernel avec `X-Api-Key`.

## Flux principaux

### Auth
- `POST /api/auth/login` → proxy kernel → JWT RS256
- `POST /api/auth/forgot-password` → kernel `/api/auth/forgot-password` + `/api/auth/password-reset/issue`
- `POST /api/auth/reset-password` → kernel `/api/auth/reset-password` (token kernel)
- Inscription artiste → sign-up kernel + création org + plan commercial + profil `Artist` local
- Inscription acheteur → sign-up kernel `PROSPECT` + `AppUser` local
- Inscription admin (`POST /api/admin/auth/register`) → sign-up kernel + rôle `TENANT_ADMIN` via `/api/administration/*` (clé API client admin requise)

### Commerce
- Produits/commandes délégués au kernel (`KernelCommerceService`)
- Paiement CamPay local → à succès : `POST /api/sales/orders/{id}/confirm` sur le kernel
- Événements Kafka `SALES_ORDER_CONFIRMED` / `SALES_ORDER_CANCELLED` → sync statut commande locale

### Fichiers
- `POST /api/artworks/images/upload` (multipart) → `POST /api/files` kernel
- `POST /api/me/profile-picture` (multipart) → `POST /api/files` kernel (`PROFILE_PICTURE`)
- URL retournée : `{KSM_KERNEL_BASE_URL}/api/files/{fileId}`

### Notifications
- Source unique : kernel (`POST /api/notifications/deliveries` + `GET /api/notifications/deliveries`)
- Plus de double écriture en base locale YowPainter

## Architecture hexagonale

```
shared/kernel/
  port/          → KernelAuthPort, KernelOrganizationPort, KernelProductPort, ...
  adapter/       → Implémentations HTTP (RestClient)
  event/         → Consommation Kafka (optionnel)
config/          → KernelProperties, KernelKafkaProperties, KernelSecurityConfig
```

## Migration données legacy

Pour backfiller `organization_id` sur artworks/produits/commandes existants :

1. Chaque artiste doit avoir `organization_id` renseigné (via inscription kernel ou admin).
2. Activer `KSM_KERNEL_LEGACY_MIGRATION_ENABLED=true` **une seule fois** au démarrage.
3. Désactiver ensuite la variable.

> Les anciennes données multi-schémas nécessitent une migration manuelle ou une réinscription des artistes via le kernel.

## Tests locaux

```bash
mvn test
```

## Endpoints kernel consommés

| Domaine | Endpoints |
|---------|-----------|
| Auth | `/api/auth/sign-up`, `/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout`, `/api/auth/forgot-password`, `/api/auth/password-reset/issue`, `/api/auth/reset-password` |
| Administration | `/api/administration/roles`, `/api/administration/roles/defaults`, `/api/administration/users/{id}/roles` |
| Org | `/api/organizations`, plans commerciaux |
| Produits | `/api/products` |
| Ventes | `/api/sales/orders`, `.../confirm`, `.../cancel` |
| Fichiers | `/api/files` |
| Notifications | `POST/GET /api/notifications/deliveries` |
