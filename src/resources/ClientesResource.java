package resources;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import dao.Actividad;
import dao.Alquiler;
import dao.Cliente;
import dao.Propiedad;
import utilities.Database;

@Path("/clientes")
public class ClientesResource {

	@Context
	UriInfo	uriInfo;

	@Context
	Request	request;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Cliente> getClients() {
		List<Cliente> clients = new ArrayList<Cliente>();
		try {
			Database.getInstance().createConnection();
			ResultSet rs = Database.getInstance().consult("select * from cliente");
			while (rs.next()) {

				Cliente cliente = new Cliente(rs.getInt("dni"), rs.getString("nombre"), rs.getString("apellido"), rs.getString("email"),
						rs.getString("direccion"), rs.getInt("cp"), rs.getInt("telefono"));

				// Se obtienen los alquileres de los clientes
				ResultSet rs2 = Database.getInstance().consult("select * from alquiler where dniCliente = " + cliente.getDni());
				while (rs2.next()) {
					// Se obtiene la actividad del alquiler
					ResultSet rs3 = Database.getInstance().consult("select * from actividad where idActividad = " + rs2.getInt("idActividad"));
					Actividad actividad = new Actividad();
					if (rs3.next()) {
						actividad.setId(rs3.getInt("idActividad"));
						actividad.setNombre(rs3.getString("nombre"));
					}
					// Se obtiene la propiedad del alquiler
					rs3 = Database.getInstance().consult("select * from propiedad where idPropiedad = " + rs2.getInt("idPropiedad"));
					Propiedad propiedad = new Propiedad();
					if (rs3.next()) {
						propiedad.setId(rs3.getInt("idPropiedad"));
						propiedad.setNombre(rs3.getString("nombre"));
					}
					cliente.getAlquileres()
							.add(new Alquiler(actividad, propiedad, rs.getDate("fechaInicio"), rs.getDate("fechaFin"), rs.getDouble("precio")));
				}
				clients.add(cliente);
			}
			Database.getInstance().disconnect();
			return clients;
		} catch (SQLException e) {
			e.printStackTrace();
			return clients;
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response newClient(Cliente client) {
		Response res = null;
		Database.getInstance().createConnection();
		if (Database.getInstance().count("cliente", "dni = " + client.getDni()) > 0) {
			res = Response.status(409).entity("Post: Client with " + client.getDni() + " already exists").build();
		} else {
			URI uri = uriInfo.getAbsolutePathBuilder().path(Integer.toString(client.getDni())).build();
			res = Response.created(uri).entity(client).build();
			Database.getInstance()
					.update("insert into cliente values (" + client.getDni() + ", '" + client.getNombre() + "', '" + client.getApellido() + "', '"
							+ client.getEmail() + "', '" + client.getDireccion() + "', " + client.getCodigoPostal() + ", " + client.getTelefono()
							+ ")");
		}
		Database.getInstance().disconnect();
		return res;
	}

	@Path("{dni}")
	public ClienteResource getTodo(@PathParam("dni") String dni) {
		return new ClienteResource(dni);
	}
}