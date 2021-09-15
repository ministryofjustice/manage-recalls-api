{{/* vim: set filetype=mustache: */}}
{{/*
Volumes for deployments
*/}}
{{- define "deployment.volumes" -}}
{{- $appName := include "generic-service-with-traefik.name" . -}}
{{- if or .Values.traefikProxy.enabled .Values.namespace_secrets_to_file -}}
volumes:
{{- if .Values.traefikProxy.enabled }}
  - name: traefik-config
    configMap:
      name: {{ include "generic-service-with-traefik.fullname" . }}
      items:
        - key: traefik.yaml
          path: traefik.yaml
        - key: traefik-conf.yaml
          path: traefik-conf.yaml
{{- end }}
{{- range $secret, $envs := .Values.namespace_secrets_to_file }}
  - name: vol-{{ $secret }}
    secret:
      secretName: {{ $secret }}
      items:
  {{- range $key, $val := $envs }}
      {{- range $val }}
      - key: {{ . }}
        path: {{ . }}
      {{- end }}
  {{- end }}
{{- end }}
{{- end }}
{{- end -}}
