
import json

#
# parameters to be passed in
#
containerImageName = "liefeld/r313_java_cli"
manifestFilePath = './manifest'
outputDefinitionPath = "./"


comment_char = '#'
sep="="
manifest = {}
with open(manifestFilePath, "rt") as f:
    for line in f:
        l = line.strip()
        if l and not l.startswith(comment_char):
            key_value = l.split(sep)
            key = key_value[0].strip()
            value = sep.join(key_value[1:]).strip().strip('"') 
            manifest[key] = value 


print(manifest)
#
# Load the module manifest and extract the module parameter names and default values
#

i = 0
keepGoing = True
params = []
defaults = {}
while (keepGoing == True):
    i = i+1
    try:
        pName = manifest['p'+str(i)+"_name"]
        pDefault = manifest['p'+str(i)+"_default_value"]
        
        params.append(pName)
        defaults[pName] = pDefault
        
    except KeyError:
        keepGoing=False    

#
# Load the jobdef template and fill in the new parameters for the module
#
with open('jobdef.json') as data_file:    
    jobdef = json.load(data_file)

for param in params: 
    jobdef['parameters'][param] = defaults[param]
    jobdef['containerProperties']['command'].append("Ref::" + param)
    
jobdef['jobDefinitionName'] = manifest['name']    

#
# Get the command line and try to make it proper for AWS
#
end = manifest['commandLine'].find('<' + params[0])
commands = (manifest['commandLine'][0:end-1]).split()

jobdef['parameters']['exe1'] = commands[0][1:-1]
jobdef['parameters']['exe2'] = commands[1]

jobdef['containerProperties']['image'] = containerImageName


text_file = open(outputDefinitionPath + "jobref."+manifest['name']+".json", "w")
text_file.write(json.dumps(jobdef, sort_keys=True, indent=4, separators=(',', ': ')))
text_file.close()        


