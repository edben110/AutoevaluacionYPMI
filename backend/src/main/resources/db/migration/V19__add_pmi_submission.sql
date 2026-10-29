-- Los PMI existentes permanecen como borradores hasta el envío explícito institucional.
ALTER TABLE pmi ADD COLUMN submitted_at TIMESTAMP WITH TIME ZONE;
