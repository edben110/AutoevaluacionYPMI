-- Textos institucionales del Anexo 1 del formato Excel revisado el 6 de octubre de 2026.
-- Juan confirmó que se conservan en el formato actualizado; la aptitud por estado está pendiente.
-- Se mantienen las valoraciones y evidencias existentes; los textos anteriores quedan en NULL.
ALTER TABLE component_valuation
    ADD COLUMN strengths TEXT,
    ADD COLUMN improvement_opportunities TEXT;
