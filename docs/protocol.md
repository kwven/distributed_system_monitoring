## Protocole Agent ↔ Serveur (Version initiale)

But : définir un **contrat clair** entre Agent et Serveur pour permettre le travail parallèle.

---

## 0) Informations à décider ???????
- Encodage : UTF-8 (recommandé)
- Format messages : [ ] JSON / [ ] autre : ___
- Ports par défaut :
  - UDP métriques : TODO
  - TCP alertes : TODO
- Identifiant agent : format (ex: `agent-001`, `pc-01`) : TODO
- Fréquence métriques : TODO (ex: 1s, 2s…)

> Une fois ces champs remplis, Agent et Serveur peuvent avancer indépendamment.

---

## 1) Champs communs (obligatoires)
Chaque message doit contenir au minimum :
- `type` : `"metrics"` ou `"alert"`
- `agentId` : identifiant unique
- `timestamp` : date/heure d’émission (format à préciser)

Exemple minimal (structure) :
```json
{
  "type": "metrics",
  "agentId": "agent-001",
  "timestamp": "TODO"
}
