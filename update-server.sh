#!/bin/bash
lein do clean, uberjar
cp .lein-env profiles.clj project.clj target/uberjar/shopping.latest.jar ~/shopping
ssh server "sudo systemctl restart shopping"

