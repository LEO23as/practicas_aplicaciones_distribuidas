# ANALISIS.md — RegistroDistribuido
**ISR-701 Aplicaciones Distribuidas — Examen Práctico Unidad 1**  
**Estudiante:** Pedro Leonardo Castro López  
**Período:** Regular 2025-2026 SPA

---

## Pregunta 1: Teorema CAP — ¿Qué propiedad privilegia la implementación?

Ante una partición de red entre dos nodos, la implementación privilegia la **Disponibilidad (A)** sobre la Consistencia (C).

**Justificación con el prototipo:**  
Cuando N1 y N3 no pueden comunicarse, ambos siguen aceptando operaciones de clientes sin esperar confirmación del otro nodo. El `NodeService` no bloquea las operaciones si algún peer no responde durante la replicación (`sendNoReply` captura la excepción silenciosamente). Esto garantiza que el sistema siga operando, pero dos nodos podrían registrar eventos con distinto estado temporal hasta que la partición se repare.

El sistema **sí garantiza tolerancia a particiones (P)** — es imposible renunciar a P en un sistema distribuido real. El teorema CAP implica que debemos elegir entre CP o AP; este prototipo elige **AP**: disponible ante fallos, eventualmente consistente vía replicación.

---

## Pregunta 2: Falacias de la computación distribuida

Al diseñar el sistema se enfrentaron directamente estas falacias:

1. **"La red es confiable"** → Falacia que afectó el framing de mensajes. Sin delimitar correctamente (4 bytes de longitud + payload JSON), dos operaciones enviadas seguidas se solaparían en el buffer TCP. Se implementó framing explícito para eliminar este riesgo.

2. **"La latencia es cero"** → Falacia que afectó los tiempos de heartbeat. Si el timeout fuera demasiado corto (por ejemplo, 200 ms), un nodo lento pero vivo podría marcarse como caído erróneamente. Se configuró `heartbeatTimeoutMs=6000` con un intervalo de envío de 2000 ms para dar margen real.

3. **"La red es homogénea"** → Falacia considerada al no asumir que todos los nodos tienen la misma capacidad de respuesta; el cliente TCP tiene `setSoTimeout(1000)` para no bloquearse indefinidamente.

4. **"El transporte es seguro"** → Falacia abordada en Parte E: sin token, cualquier mensaje es rechazado, ya que no se puede asumir que solo clientes legítimos llegan al puerto.

---

## Pregunta 3: Tipos de transparencia

| Tipo | ¿La ofrece? | Argumento |
|------|-------------|-----------|
| **Ubicación** | Parcialmente | El cliente se conecta a un nodo específico por host:puerto. No hay un punto de entrada único que oculte la ubicación real de los nodos. |
| **Acceso** | Sí | El cliente usa siempre el mismo protocolo TCP + framing + JSON independientemente del nodo destino. No distingue si está hablando con N1, N2 o N3. |
| **Fallos** | Parcialmente | Los nodos detectan la caída de peers y continúan operando. Sin embargo, el cliente recibe error si el nodo al que se conectó está caído — no hay redirección automática. |
| **Replicación** | No | El cliente sabe que manda al nodo N1, que luego replica. No existe transparencia de replicación porque si N1 cae antes de replicar, el cliente lo nota. |

---

## Pregunta 4: SLA de disponibilidad

**SLA propuesto: 99,9% ("tres nueves")**

Cálculo del tiempo de inactividad anual admisible:

```
Horas/año = 365 × 24 = 8.760 horas
Tiempo inactivo permitido = 8.760 × (1 - 0,999) = 8,76 horas/año
                           ≈ 8 horas 45 minutos al año
                           ≈ 43,8 minutos al mes
                           ≈ 10,1 minutos a la semana
```

**Justificación:** Con 3 nodos réplica y tolerancia a la caída de 1 nodo, el sistema puede absorber mantenimientos planificados y fallos espontáneos sin superar ese umbral. Un 99,99% ("cuatro nueves", ~52 min/año) requeriría failover automático del cliente, lo cual este prototipo no implementa.

---

## Pregunta 5: Reemplazar Bully por consenso tipo Raft

### Qué se ganaría con Raft:

- **Consistencia fuerte (CP):** Raft garantiza que solo el líder acepta escrituras y las replicas confirman antes de responder al cliente. Bully solo elige un coordinador, pero no garantiza consistencia de datos.
- **Log replicado:** Raft mantiene un log de entradas replicado en mayoría de nodos (quorum), previniendo pérdida de datos ante fallos del líder.
- **Elecciones más robustas:** Bully puede generar múltiples elecciones simultáneas bajo particiones; Raft usa mandatos (terms) y votos para evitarlo.

### Qué costo introduciría:

- **Mayor complejidad:** Raft requiere manejo de mandatos, votación, replicación de log en dos fases, snapshots. El código sería 5-10x más extenso.
- **Mayor latencia de escritura:** Cada operación requiere confirmación de mayoría (quorum = 2/3 nodos) antes de responder al cliente.
- **Overhead de mensajes:** 3 nodos × mensajes AppendEntries + heartbeats + RequestVote en cada elección vs. los simples ELECTION/COORDINATOR del Bully.

**Conclusión:** Para este prototipo académico con enfoque en demostrar mecanismos, Bully es suficiente. En producción con datos críticos, Raft es la elección correcta.
