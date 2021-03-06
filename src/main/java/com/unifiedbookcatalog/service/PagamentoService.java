package com.unifiedbookcatalog.service;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.servlet.ServletContext;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.unifiedbookcatalog.daos.CompraDAO;
import com.unifiedbookcatalog.models.Compra;

@Path("/pagamento")
public class PagamentoService {
	
	@Inject
    private CompraDAO compraDAO;
	
	@Inject
	private PagamentoGateway pagamentoGateway;
	
	@Context
	private ServletContext context;
	
	@Inject
	private JMSContext jmsContext;
	
	private static ExecutorService executor = Executors.newFixedThreadPool(50);
	
	@Resource(name="java:/jms/topics/CarrinhoComprasTopico")
	private Destination destination;
	
	@POST
	public void pagar(@Suspended final AsyncResponse ar, @QueryParam("uuid") String uuid) {
		Compra compra = compraDAO.buscaPorUuid(uuid);
		String contextPath = context.getContextPath(); 
		JMSProducer producer = jmsContext.createProducer();
		

	    executor.submit(() -> {
	    	 try {
		    	String resposta = pagamentoGateway.pagar(compra.getTotal());
		    	producer.send(destination, compra.getUuid());
		    	URI responseUri= UriBuilder.fromPath("http://localhost:8080" + contextPath + "/index.xhtml").queryParam("msg", "Compra Realizada com Sucesso").build();
		    	Response response = Response.seeOther(responseUri).build();
		    	ar.resume(response);
	    	 } catch(Exception e) {
	    		 ar.resume(new WebApplicationException(e));
	    	 }
	    });

	}
}