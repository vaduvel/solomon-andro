# Solomon Android MVP 2026 — Play Compliance Decisions

## Release Decision

The Play Store MVP must ship without SMS permissions.

Release allowed ingestion paths:

- Notification Listener for bank notifications
- Share Intent for user-initiated receipt/text import
- Manual transaction entry
- Email forwarding/parser path when implemented without restricted Gmail scopes

Debug/internal only:

- SMS receiver
- `READ_SMS`
- `RECEIVE_SMS`

## Why SMS Is Debug-Only

Google Play SMS/Call Log policy is high-risk for non-default SMS apps. Personal finance SMS parsing may be reviewed as a narrow exception, but approval is uncertain and could block the MVP.

Current implementation:

- `app/src/main/AndroidManifest.xml` declares no SMS permissions
- `app/src/main` contains no `SmsPaymentReceiver`
- `app/src/debug/AndroidManifest.xml` declares `READ_SMS` / `RECEIVE_SMS`
- `app/src/debug/java/.../SmsPaymentReceiver.kt` keeps the internal test receiver

## Play Review Positioning

Solomon is not a lender, broker, investment platform, payment initiator, or financial advisor. It is a local-first budgeting and transaction insight app.

Allowed wording:

- "estimare"
- "analiză informativă"
- "asistent digital"
- "nu constituie consiliere financiară"
- "datele rămân pe telefon"

Avoid wording:

- "consilier financiar autorizat"
- "garanție"
- "decizie sigură"
- "recomandare de credit"
- "aplică pentru credit"
- "investește în"

## Permission Copy

Notification Listener:

> Solomon folosește această permisiune pentru a detecta automat tranzacțiile din notificările tale bancare (de ex. BT Pay, George, Revolut) și pentru a-ți afișa rapoarte și alerte personalizate. Nu colectăm notificări non-financiare și poți oricând dezactiva acest acces.

Microphone:

> Permisiunea de microfon permite înregistrarea comenzilor vocale pentru a adăuga tranzacții sau a interacționa cu chatbotul. Înregistrarea se procesează local și nu este stocată.

Cloud AI:

> Dacă activezi modul AI cloud, anumite informații despre tranzacțiile tale pot fi trimise criptat către Mistral pentru a genera răspunsuri. Poți dezactiva oricând această opțiune.

## Data Safety Draft

Declare:

- Financial info: transactions, obligations, subscriptions, goals
- App activity: only if in-app analytics/logs are added later
- Audio: microphone access for voice input, not stored
- No contacts, location, photos, calendar, or advertising IDs
- Cloud AI data sharing: optional and user-controlled

Do not declare SMS for release unless SMS is reintroduced after policy approval.

## MVP Must-Ship

- Notification Listener consent screen
- Share Intent import
- Manual add transaction
- Safe-to-spend
- Subscription audit
- Spiral/IFN warning as educational insight
- Privacy Policy link
- Data Safety form aligned with release permissions

## Remove Before Play Review

- Any release manifest SMS permissions
- Any production UI asking for SMS access
- Claims of financial advice
- Credit marketplace / loan referral links
- Unimplemented Open Banking login CTA that implies live bank sync
