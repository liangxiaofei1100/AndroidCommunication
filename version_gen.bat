@echo off
rem This bat is used for auto generating version code. The version code formate is "version.date.time".
rem Created time: 20130924

set file=res\values\version.xml
set version_name=1.0.0
set build_time=%date:~0,4%%date:~5,2%%date:~8,2%.%time:~0,2%%time:~3,2%%time:~6,2%

echo ^<?xml version="1.0" encoding="utf-8"?^> > %file%
echo ^<resources^> >> %file%
echo ^<string name="version_name"^>%version_name%.%build_time%^</string^> >> %file%
echo ^</resources^> >> %file%