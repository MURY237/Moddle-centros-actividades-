#!/usr/bin/env bash
# Firma un APK con la clave propia SIN perder a los usuarios que ya tienen la app.
#
# La app se firmó al principio con debug.keystore, cuya contraseña es pública. Android no
# deja actualizar una app con otra clave... salvo que el APK lleve una «cadena de rotación»
# (APK Signature Scheme v3) que diga «la clave antigua autoriza a esta nueva». Entonces
# Android 9 o superior conserva la app y sus datos al actualizar.
#
# Se firma con las dos claves: la antigua (para Android 8, que no entiende la cadena) y la
# nueva con la cadena. La clave antigua NO conserva el permiso de volver atrás, así que
# quien la tiene —es pública— no puede colar un APK sobre una instalación ya actualizada.
#
# Uso: firmar-con-rotacion.sh <apk de entrada> <apk de salida> <almacén nuevo>
# Variables: FIRMA_ALIAS, FIRMA_CLAVE_ALMACEN, FIRMA_CLAVE (las del almacén nuevo)
set -euo pipefail

entrada=$1
salida=$2
nuevo=$3
viejo=${ALMACEN_ANTIGUO:-debug.keystore}

sdk=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}
apksigner=$(ls -d "$sdk"/build-tools/*/ | sort -V | tail -1)apksigner

cadena=$(mktemp -u)
trap 'rm -f "$cadena"' EXIT

"$apksigner" rotate --out "$cadena" \
  --old-signer --ks "$viejo" --ks-pass pass:android --ks-key-alias androiddebugkey --key-pass pass:android \
  --new-signer --ks "$nuevo" --ks-pass env:FIRMA_CLAVE_ALMACEN --ks-key-alias "$FIRMA_ALIAS" --key-pass env:FIRMA_CLAVE

"$apksigner" sign --lineage "$cadena" --out "$salida" \
  --ks "$viejo" --ks-pass pass:android --ks-key-alias androiddebugkey --key-pass pass:android \
  --next-signer \
  --ks "$nuevo" --ks-pass env:FIRMA_CLAVE_ALMACEN --ks-key-alias "$FIRMA_ALIAS" --key-pass env:FIRMA_CLAVE \
  "$entrada"

informe=$("$apksigner" verify --verbose --print-certs "$salida")
echo "$informe" | grep -E "^Verified|^Number of signers|certificate SHA-256 digest" || true
echo "$informe" | grep -q "Verified using v3 scheme (APK Signature Scheme v3): true" \
  || { echo "El APK no verifica con la firma v3 (la de la rotación)."; exit 1; }
echo "$informe" | grep -q "Verified using v2 scheme (APK Signature Scheme v2): true" \
  || { echo "El APK no verifica con la firma v2."; exit 1; }
echo "Firmado con rotación: $salida"
