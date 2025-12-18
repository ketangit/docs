#!/bin/bash
set -euo pipefail

MODE="${MODE:-web}"   # default to web mode

if [ "$MODE" = "web" ]; then
  echo "Starting Spring Boot web application..."
  exec java -jar /opt/app.jar
elif [ "$MODE" = "worker" ]; then
  RUN_ID="${RUN_ID:-}"
  SCENARIO="${SCENARIO:-}"
  RESULTS_PATH="${RESULTS_PATH:-/var/gatling/results}"
  S3_BUCKET="${S3_BUCKET:-}"
  AWS_REGION="${AWS_REGION:-us-east-1}"

  if [ -z "$RUN_ID" ] || [ -z "$SCENARIO" ]; then
    echo "RUN_ID and SCENARIO must be provided in worker mode" >&2
    exit 2
  fi

  mkdir -p "${RESULTS_PATH}/${RUN_ID}"
  LOG_FILE="${RESULTS_PATH}/${RUN_ID}/run.log"

  echo "Starting Gatling run ${RUN_ID} scenario ${SCENARIO}" | tee -a "$LOG_FILE"

  echo "1" | "${GATLING_HOME}/bin/gatling.sh" -s "${SCENARIO}" -rf "${RESULTS_PATH}/${RUN_ID}" 2>&1 | tee -a "$LOG_FILE"
  GATLING_EXIT=${PIPESTATUS[0]:-0}

  echo "Gatling finished with exit code ${GATLING_EXIT}" | tee -a "$LOG_FILE"
  touch "${RESULTS_PATH}/${RUN_ID}/DONE"

  if [ -n "${S3_BUCKET}" ]; then
    echo "Uploading results to s3://${S3_BUCKET}/${RUN_ID}/" | tee -a "$LOG_FILE"
    aws --region "${AWS_REGION}" s3 cp "${RESULTS_PATH}/${RUN_ID}/" "s3://${S3_BUCKET}/${RUN_ID}/" --recursive 2>&1 | tee -a "$LOG_FILE"
    echo "Upload complete" | tee -a "$LOG_FILE"
  fi

  exit ${GATLING_EXIT}
else
  echo "Unknown MODE: $MODE" >&2
  exit 2
fi
