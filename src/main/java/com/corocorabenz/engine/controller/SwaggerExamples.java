package com.corocorabenz.engine.controller;

/** JSON examples shown by Swagger UI for the learning demo. */
final class SwaggerExamples {

    private SwaggerExamples() {
    }

    static final String TI_START = "{\"businessKey\":\"TI-DEMO-001\",\"variables\":{\"requesterId\":\"USR-01\",\"systemName\":\"ERP\",\"description\":\"No permite iniciar sesión\",\"impact\":\"Medio\",\"urgency\":\"Media\",\"safetyImpact\":false,\"affectedTeams\":1}}";

    static final String POSTVENTA_START = "{\"businessKey\":\"POST-DEMO-001\",\"variables\":{\"vin\":\"CB0TEST0000000001\",\"customerName\":\"Cliente de prueba\",\"serviceType\":\"Garantía\",\"mileageKm\":12000,\"symptom\":\"Ruido en motor\"}}";

    static final String NOMINA_START = "{\"businessKey\":\"NOM-DEMO-001\",\"variables\":{\"payPeriodId\":\"2026-11\",\"payCycle\":\"MENSUAL\",\"cutoffDate\":\"2026-11-30\"}}";

    static final String TRIAGE_TASK = "{\"variables\":{\"technicalDomain\":\"catalogoNivel1\"}}";

    static final String DIAGNOSIS_TASK = "{\"variables\":{\"rootCause\":\"Error de acceso en el sistema\",\"resolutionSummary\":\"Se restablecieron las credenciales\"}}";

    static final String CONFIRMATION_TASK = "{\"variables\":{\"userConfirmed\":true}}";

    static final String EXTERNAL_TASK_LOCK = "{\"workerId\":\"demo-worker\",\"lockDurationMs\":60000}";

    static final String EXTERNAL_TASK_COMPLETE = "{\"workerId\":\"demo-worker\",\"variables\":{\"ticketStatus\":\"CLOSED\"}}";

    static final String MESSAGE_EVENT = "{\"businessKey\":\"POST-DEMO-001\",\"variables\":{}}";
}
