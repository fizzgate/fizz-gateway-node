#!/bin/bash

# entrypoint for docker

/bin/bash ./boot.sh start && touch dummy.log && tail -f dummy.log 