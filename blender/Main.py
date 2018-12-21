import http.client
import http.server
import json
# noinspection PyUnresolvedReferences
import sys
import threading as th

import bpy


def generateMeshFromFaceIndices(vertices, faceIndices):
    myMesh = bpy.data.meshes.new('MyMesh')
    myObject = bpy.data.objects.new('MyObject', myMesh)

    scene = bpy.context.scene
    scene.objects.link(myObject)
    scene.objects.active = myObject
    myObject.select = True

    myMesh.from_pydata(vertices, [], faceIndices)


def generateMeshFromFaces(faces):
    unpackedFaces = [[(vertex['x'], vertex['y'], vertex['z']) for vertex in face] for face in faces]
    vertices = []
    faceIndices = []

    for face in unpackedFaces:
        for vertex in face:
            if vertex not in vertices:
                vertices.append(vertex)

    for face in unpackedFaces:
        faceIndices.append([vertices.index(vertex) for vertex in face])

    generateMeshFromFaceIndices(vertices, faceIndices)


def deleteMesh():
    myObject = bpy.data.objects['MyObject']
    bpy.data.objects.remove(myObject, True)


def getMesh():
    return bpy.data.objects['MyObject'].data


def unpackMeshAsFaceIndices():
    myMesh = getMesh()
    vertices = [v.co[:] for v in myMesh.vertices]
    faceIndices = [f.vertices[:] for f in myMesh.polygons]

    return vertices, faceIndices


def unpackMeshAsFaces():
    vertices, faceIndices = unpackMeshAsFaceIndices()
    faces = []

    for indices in faceIndices:
        faces.append([
            {'x': vertices[index][0], 'y': vertices[index][1], 'z': vertices[index][2]}
            for index in indices
        ])

    return faces


def createMesh():
    with open(bpy.path.abspath('//Beethoven.obj')) as p:
        lines = p.readlines()

    vertices = []
    faces = []

    for n in lines:
        tokens = n.split()

        if len(tokens) == 0:
            continue

        part = tokens[0]

        if part == 'v':
            vertices.append((
                float(tokens[1]),
                float(tokens[2]),
                float(tokens[3])
            ))
        elif part == 'f':
            faces.append(tuple(
                int(tokens[i].split('/')[0]) - 1
                for i in range(1, len(tokens))
            ))

    generateMeshFromFaceIndices(vertices, faces)


def meshExists():
    try:
        _ = bpy.data.objects['MyObject']
        return True
    except KeyError:
        return False


# ===================================================

class MyHandler(http.server.BaseHTTPRequestHandler):
    def do_POST(self):
        data = self.rfile.read(int(self.headers['Content-Length']))

        if meshExists():
            deleteMesh()

        snapshot = json.loads(data.decode())
        generateMeshFromFaces(snapshot['faces'])

        self.setHeaders()

    def log_message(self, *args, **kwargs):
        sys.stderr.write('Log: ')
        super().log_message(*args, **kwargs)

    def setHeaders(self):
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()


def runServer(myPort):
    httpd = http.server.HTTPServer(('', myPort), MyHandler)
    httpd.serve_forever()


# ===================================================

def runClient(bcPort):
    connection = http.client.HTTPConnection('127.0.0.1', bcPort)

    while 1:
        command = input('Command: ').strip()

        if command == 'create':
            if meshExists():
                print('Mesh already exists!')
            else:
                createMesh()
        elif command == 'commit':
            if meshExists():
                faces = unpackMeshAsFaces()

                data = json.dumps({
                    'faces': faces
                })

                connection.request(
                    'POST',
                    '/model',
                    data.encode(),
                    {'Content-type': 'application/json'}
                )

                connection.getresponse().read()
            else:
                print('Mesh doesn\'t exist!')
        else:
            generateMeshFromFaces([
                [{'x': 0, 'y': 0, 'z': 0}, {'x': 1, 'y': 0.5, 'z': 0}, {'x': 1, 'y': 1, 'z': 0.5}]
            ])
            print('Invalid command!')


# ===================================================

def runMain():
    if '--' in sys.argv:
        myPort = int(sys.argv[-2])
        bcPort = int(sys.argv[-1])
    else:
        myPort = int(input('My port: '))
        bcPort = int(input('Blockchain client port: '))

    print(
        '+-------------------------------------+\n'
        '| My port: %04d                       |\n'
        '| Blockchain client port: %04d        |\n'
        '|                                     |\n'
        '| Commands:                           |\n'
        '| - create: Create a new mesh.        |\n'
        '| - commit: Commit the current mesh.  |\n'
        '+-------------------------------------+\n'
        % (myPort, bcPort)
    )

    th.Thread(target=runServer, args=(myPort,)).start()
    th.Thread(target=runClient, args=(bcPort,)).start()


# ===================================================

th.Thread(target=runMain).start()
