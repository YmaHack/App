!macro customInstall
  nsExec::ExecToLog 'netsh advfirewall firewall add rule name="אתגר יומי Sync" dir=in action=allow protocol=TCP localport=39225 profile=private'
!macroend

!macro customUnInstall
  nsExec::ExecToLog 'netsh advfirewall firewall delete rule name="אתגר יומי Sync"'
!macroend
