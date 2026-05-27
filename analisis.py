# Autor: Castro Lopez Pedro - pcastrol@uteq.edu.ec

import os
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from tabulate import tabulate   # pip install tabulate

# ── 1. TABLA COMPARATIVA TCP vs UDP ─────────────────────────────────────────
tabla = [
    ["Orientado a conexión",      "Sí (handshake 3 vías)",   "No (sin conexión)"],
    ["Confiabilidad",             "Alta (ACK + reenvío)",    "Baja (sin garantía)"],
    ["Orden de paquetes",         "Garantizado",             "No garantizado"],
    ["Control de flujo",          "Sí (ventana deslizante)", "No"],
    ["Velocidad relativa",        "Más lento",               "Más rápido"],
    ["Latencia promedio (ms)",    "~12 ms",                  "~4 ms"],
    ["Uso típico",                "HTTP, FTP, e-mail",       "DNS, streaming, juegos"],
    ["Puerto usado (práctica)",   "9000",                    "9001"],
]

encabezados = ["Característica", "TCP", "UDP"]
print("\n═══════════ TABLA COMPARATIVA TCP vs UDP ═══════════")
print(tabulate(tabla, headers=encabezados, tablefmt="fancy_grid"))

# ── 2. BOXPLOT DE LATENCIA SIMULADA ─────────────────────────────────────────
np.random.seed(42)
latencia_tcp = np.random.normal(loc=12, scale=3, size=100)   # ms
latencia_udp = np.random.normal(loc=4,  scale=1.5, size=100) # ms

# Crear carpeta si no existe
os.makedirs("docs/figures", exist_ok=True)

fig, ax = plt.subplots(figsize=(8, 5))
bp = ax.boxplot(
    [latencia_tcp, latencia_udp],
    labels=["TCP (puerto 9000)", "UDP (puerto 9001)"],
    patch_artist=True,
    medianprops=dict(color="black", linewidth=2),
)

colores = ["#4A90D9", "#E87040"]
for patch, color in zip(bp["boxes"], colores):
    patch.set_facecolor(color)
    patch.set_alpha(0.75)

ax.set_title("Comparativa de Latencia: TCP vs UDP\n(datos simulados – ISR-701)", fontsize=13)
ax.set_ylabel("Latencia (ms)")
ax.set_xlabel("Protocolo")
ax.grid(axis="y", linestyle="--", alpha=0.5)

leyenda = [
    mpatches.Patch(color="#4A90D9", alpha=0.75, label="TCP – confiable, más lento"),
    mpatches.Patch(color="#E87040", alpha=0.75, label="UDP – rápido, sin garantía"),
]
ax.legend(handles=leyenda, loc="upper right")

ruta = "docs/figures/boxplot_latencia.png"
plt.tight_layout()
plt.savefig(ruta, dpi=150)
print(f"\n✔ Figura guardada en: {ruta}")
plt.show()