# WarrantyBox

Aplicação Android offline para organizar compras, faturas, garantias, documentos e reparações.

## Tecnologia

- Kotlin, Jetpack Compose e Material 3
- Room + Repository + MVVM
- WorkManager para alertas locais
- Android Biometric API
- Storage Access Framework para imagens, PDFs, backups e restauro

## Executar

Abra a pasta no Android Studio (JDK 17) e execute a configuração `app`, ou use:

```bash
./gradlew assembleDebug
```

Os dados e documentos permanecem no dispositivo. O módulo `ocr` define o contrato para futura extração local de faturas; os resultados nunca são guardados sem confirmação.

## Estado da versão 1.0

Inclui registo e edição de compras, dashboard, pesquisa e ordenação, estados de garantia, alertas 90/30/7/1 dias, detalhe, histórico de reparações, relatório de assistência partilhável, estatísticas, temas, traduções PT/FR/EN, proteção biométrica e backup JSON.
