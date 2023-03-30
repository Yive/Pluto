#!/usr/bin/env bash

# requires curl & jq

# upstreamCommit <baseHash> <newHash>
# param: bashHash - the commit hash to use for comparing commits (baseHash...newHash)
# param: newHash - the commit hash to use for comparing commits

(
set -e
PS1="$"

pufferfish=$(curl -H "Accept: application/vnd.github.v3+json" https://api.github.com/repos/pufferfish-gg/Pufferfish/compare/$1...$2 | jq -r '.commits[] | "pufferfish-gg/Pufferfish@\(.sha[:7]) \(.commit.message | split("\r\n")[0] | split("\n")[0])"')

updated=""
logsuffix=""
if [ ! -z "pufferfish" ]; then
    logsuffix="$logsuffix\n\nPufferfish Changes:\n$pufferfish"
    updated="Pufferfish"
fi
disclaimer="Upstream has released updates that appear to apply and compile correctly"

log="${UP_LOG_PREFIX}Updated Upstream ($updated)\n\n${disclaimer}${logsuffix}"

echo -e "$log" | git commit -F -

) || exit 1