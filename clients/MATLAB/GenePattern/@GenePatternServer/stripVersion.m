function [versionless, ver] = stripVersion(obj, id)
str = '';
id = char(id);

[token, rem] = strtok(id, ':');
str = strcat(str, token, ':')	; % urn
[token, rem] = strtok(rem, ':');
str = strcat(str, token, ':')	; %lsid
[token, rem] = strtok(rem, ':');
str = strcat(str, token, ':')	; % authority
[token, rem] = strtok(rem, ':');
str = strcat(str, token, ':')	; % namespace
[token, rem] = strtok(rem, ':');
str = strcat(str, token)	; % id

versionless = str;
[ver, rem] = strtok(rem, ':');

