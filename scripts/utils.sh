#!/bin/bash

function die {
  local msg="$1"

  echo "[ERROR]: $msg"
  exit 1
}
