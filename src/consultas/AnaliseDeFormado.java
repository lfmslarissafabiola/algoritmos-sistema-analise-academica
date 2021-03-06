package consultas;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import dao.mongo.MongoConnection;

public class AnaliseDeFormado {
	private DBCollection alunos;
	private String current_curso;
	private String id_aluno_representante;
	private List<String> list_excluidos_perfil;
	
	public AnaliseDeFormado(String curso){
		current_curso = curso;
			alunos = null;
				try {
					alunos = MongoConnection.getInstance().getDB().getCollection("atividades_academica_"+curso.toLowerCase().trim().replace(" ", "_"));
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	}
	
	public List<String> filterByFormado(String forma_saida, double porcentagemConjunto){
			DBObject match = new BasicDBObject("$match", new BasicDBObject("forma_saida", forma_saida));

			DBObject id = new BasicDBObject();
			id.put("curso","$cod_curso");
			id.put("id_aluno", "$id_aluno");
			id.put("ano_ingresso", "$ano_ingresso");
			id.put("ano_saida", "$ano_saida");
			
			DBObject groupFields = new BasicDBObject();
			groupFields.put("_id", id);
			groupFields.put("quantidade", new BasicDBObject("$sum", 1));
			DBObject group = new BasicDBObject("$group", groupFields);
						
			List<DBObject> pipeline = Arrays.asList(match, group);
			AggregationOutput output = alunos.aggregate(pipeline);
									
			List<String> listaFormandosGeral = new ArrayList<String>();
			List<String> listaAlunosConjunto = new ArrayList<String>();
			List<String> listaAlunosForaConjunto = new ArrayList<String>();

			
			for (DBObject dbo : output.results()) {
//				System.out.println("dbo: " + dbo);
				DBObject getId = (DBObject) dbo.get("_id");
				String id_aluno = getId.get("id_aluno").toString();
				int anos = Integer.parseInt(getId.get("ano_saida").toString()) - Integer.parseInt(getId.get("ano_ingresso").toString());
//				System.out.println("anos: " + anos);
				if (anos <= 3) {
					listaFormandosGeral.add(id_aluno);
				}
			}
			
//			System.out.println("lista de formandos");
//			System.out.println(listaFormandosGeral);
			
			int count = 1;
			BasicDBObject document = new BasicDBObject();
			DBCollection trainingSet = null;
			
			// training_set2 com um aumento de alunos do conjunto
			try {
				trainingSet = MongoConnection.getInstance().getDB().getCollection(
				"training_set2"+forma_saida.toLowerCase().trim().replace(" ", "").replace("(crit.01)", "").replace("(crit.02)", "02")+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			System.out.println("size");
			int size = (int)((double)listaFormandosGeral.size() * porcentagemConjunto);
			System.out.println(size);
			
			for (int i = 0; i < listaFormandosGeral.size(); i++) {
				String id_aluno = listaFormandosGeral.get(i);
				
				if (count <= size) {
					listaAlunosConjunto.add(id_aluno);
					
					List<List<String>> result = new ArrayList<List<String>>();
					result = getDetailsAluno(id_aluno);
					
					for (int item = 0; item < result.size(); item++ ) {
						document = new BasicDBObject();
						List<String> activity = result.get(item);
						
						document.put("id_aluno", id_aluno);
						document.put("nome_curso", activity.get(0));
						document.put("cod_curso", activity.get(1));
						document.put("versao_curso", activity.get(2));
						document.put("cod_ativ_curricular", activity.get(3));
						document.put("nome_ativ_curricular", activity.get(4));
						
						if (activity.get(5) != " ") {
							document.put("media_final", Double.parseDouble(activity.get(5)));
						} else {
							document.put("media_final", 0);
						}
						
						document.put("descricao_situacao", activity.get(6));
						document.put("ano", Integer.parseInt(activity.get(7)));
						document.put("periodo", activity.get(8));
						document.put("creditos", Integer.parseInt(activity.get(9)));
						
						if (activity.get(10) != " ") {
							document.put("carga_horaria_teorica", Integer.parseInt(activity.get(10)));
						} else {
							document.put("carga_horaria_teorica", 0);
						}
						
						if (activity.get(11) != " ") {
							document.put("carga_horaria_pratica", Integer.parseInt(activity.get(11)));
						} else {
							document.put("carga_horaria_pratica", 0);
						}
						
						document.put("forma_ingresso", activity.get(12));
						document.put("ano_ingresso", Integer.parseInt(activity.get(13)));
						document.put("forma_saida", activity.get(14));
						document.put("ano_saida", Integer.parseInt(activity.get(15)));

//						trainingSet.save(document);	
					}
				} else {
					listaAlunosForaConjunto.add(id_aluno);
				}
				count ++;
			}
			
			list_excluidos_perfil = listaAlunosForaConjunto;
			
			System.out.println("LISTA DE EXCLUIDOS DO PERFIL");
			System.out.println(listaAlunosForaConjunto);
			System.out.println("LISTA DE ALUNOS DO CONJUNTO");
			System.out.println(listaAlunosConjunto);
			
			return listaAlunosConjunto;

	}
	
	public List<List<String>> getDetailsAluno(String id_aluno) {
		DBObject match = new BasicDBObject("$match", new BasicDBObject("id_aluno", id_aluno));

		DBObject id = new BasicDBObject();
		id.put("id_aluno", "$id_aluno");
		id.put("nome_curso", "$nome_curso");
		id.put("cod_curso", "$cod_curso");
		id.put("versao_curso", "$versao_curso");
		id.put("cod_ativ_curricular", "$cod_ativ_curricular");
		id.put("nome_ativ_curricular", "$nome_ativ_curricular");
		id.put("media_final", "$media_final");
		id.put("descricao_situacao", "$descricao_situacao");
		id.put("ano", "$ano");
		id.put("periodo", "$periodo");
		id.put("creditos", "$creditos");
		id.put("carga_horaria_teorica", "$carga_horaria_teorica");
		id.put("carga_horaria_pratica", "$carga_horaria_pratica");
		id.put("forma_ingresso", "$forma_ingresso");
		id.put("ano_ingresso", "$ano_ingresso");
		id.put("forma_saida", "$forma_saida");
		id.put("ano_saida", "$ano_saida");
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
					
		List<DBObject> pipeline = Arrays.asList(match, group);
		AggregationOutput output = alunos.aggregate(pipeline);
								
		List<List<String>> list = new ArrayList<List<String>>();
		
		for (DBObject dbo : output.results()) {
			DBObject getId = (DBObject) dbo.get("_id");
			String nome_curso = getId.get("nome_curso").toString();
			String cod_curso = getId.get("cod_curso").toString();
			String versao_curso = getId.get("versao_curso").toString();
			String cod_ativ_curricular = getId.get("cod_ativ_curricular").toString();
			String nome_ativ_curricular = getId.get("nome_ativ_curricular").toString();
			
			String media_final = " ";
			if (getId.get("media_final") != null) {
				media_final = getId.get("media_final").toString();
			}
			
			String descricao_situacao = getId.get("descricao_situacao").toString();
			String ano = getId.get("ano").toString();
			String periodo = getId.get("periodo").toString();
			String creditos = getId.get("creditos").toString();
			
			String carga_horaria_teorica = " ";
			if (getId.get("carga_horaria_teorica") != null) {
				carga_horaria_teorica = getId.get("carga_horaria_teorica").toString();
			}
			
			String carga_horaria_pratica = " ";
			if (getId.get("carga_horaria_pratica") != null) {
				carga_horaria_pratica = getId.get("carga_horaria_pratica").toString();
			}
			
			String forma_ingresso = getId.get("forma_ingresso").toString();
			String ano_ingresso = getId.get("ano_ingresso").toString();
			String forma_saida = getId.get("forma_saida").toString();
			String ano_saida = getId.get("ano_saida").toString();
			
			List<String> listInside = new ArrayList<String>();

			listInside.add(nome_curso);
			listInside.add(cod_curso);
			listInside.add(versao_curso);
			listInside.add(cod_ativ_curricular);
			listInside.add(nome_ativ_curricular);
			listInside.add(media_final);
			listInside.add(descricao_situacao);
			listInside.add(ano);
			listInside.add(periodo);
			listInside.add(creditos);
			listInside.add(carga_horaria_teorica);
			listInside.add(carga_horaria_pratica);
			listInside.add(forma_ingresso);
			listInside.add(ano_ingresso);
			listInside.add(forma_saida);
			listInside.add(ano_saida);
			
			list.add(listInside);
			
		}
		return list;
	}
	
	public List<String> getDisciplinesDistintics(String forma_saida){
		DBObject match = new BasicDBObject("$match", new BasicDBObject("forma_saida", forma_saida));

		DBObject id = new BasicDBObject();
		id.put("cod_ativ_curricular", "$cod_ativ_curricular");
		id.put("nome_ativ_curricular", "$nome_ativ_curricular");
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
		
		DBCollection trainingSet = null;
		
		try {
			trainingSet = MongoConnection.getInstance().getDB().getCollection(
			"training_set2"+forma_saida.toLowerCase().trim().replace(" ", "").replace("(crit.01)", "").replace("(crit.02)", "02")+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
					
		List<DBObject> pipeline = Arrays.asList(match, group);
		AggregationOutput output = trainingSet.aggregate(pipeline);
								
		List<String> list = new ArrayList<String>();
		
		for (DBObject dbo : output.results()) {
//			System.out.println("dbo: " +dbo);
			DBObject getId = (DBObject) dbo.get("_id");
			String cod_ativ_curricular = getId.get("cod_ativ_curricular").toString();
			list.add(cod_ativ_curricular);
		}
		return list;
	}
	
	public List<String> getAlunos(String forma_saida){
		DBCollection trainingSet = null;
		try {
			trainingSet = MongoConnection.getInstance().getDB().getCollection(
			"training_set2"+forma_saida.toLowerCase().trim().replace(" ", "").replace("(crit.01)", "").replace("(crit.02)", "02")+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		System.out.println("training");
//		System.out.println("training_set2"+forma_saida.toLowerCase().trim().replace(" ", "").replace("(crit.01)", "")+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		
		DBObject match = new BasicDBObject("$match", new BasicDBObject("forma_saida", forma_saida));

		DBObject id = new BasicDBObject();
		id.put("id_aluno", "$id_aluno");
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
					
		List<DBObject> pipeline = Arrays.asList(match, group);
		AggregationOutput output = trainingSet.aggregate(pipeline);
								
		List<String> list = new ArrayList<String>();
		
		for (DBObject dbo : output.results()) {
			DBObject getId = (DBObject) dbo.get("_id");
			String id_aluno = getId.get("id_aluno").toString();
			list.add(id_aluno);
		}
		return list;
	}
	
	public double calculaMediaFinal(List<Double> notas) {
		double media = 0.0;
		double mediaTemp = 0.0;
		for (int i = 0; i < notas.size(); i++) {
			 mediaTemp = mediaTemp + notas.get(i);
		}
		media = mediaTemp / notas.size();
		
		return media;
	}
	
	public double getAverage(String forma_saida, String id_aluno, String cod_ativ_curricular) {
		DBCollection trainingSet = null;
		try {
			trainingSet = MongoConnection.getInstance().getDB().getCollection(
			"training_set2"+forma_saida.toLowerCase().trim().replace(" ", "").replace("(crit.01)", "").replace("(crit.02)", "02")+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		DBObject match = new BasicDBObject("$match", new BasicDBObject("forma_saida", forma_saida));
		DBObject matchAluno = new BasicDBObject("$match", new BasicDBObject("id_aluno", id_aluno));
		DBObject matchDisciplina = new BasicDBObject("$match", new BasicDBObject("cod_ativ_curricular", cod_ativ_curricular));

		DBObject id = new BasicDBObject();
		id.put("id_aluno", "$id_aluno");
		id.put("cod_ativ_curricular", "$cod_ativ_curricular");
		id.put("media_final", "$media_final");
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
					
		List<DBObject> pipeline = Arrays.asList(match, matchAluno, matchDisciplina, group);
		AggregationOutput output = trainingSet.aggregate(pipeline);
								
		double media_final = 0.0;
		
		List<Double> notas = new ArrayList<Double>();
		
		for (DBObject dbo : output.results()) {
//			System.out.println("dbo: " +dbo);
			DBObject getId = (DBObject) dbo.get("_id");
			media_final = Double.parseDouble(getId.get("media_final").toString());
			notas.add(media_final);
		}
		
		if (notas.size() > 0) {
			media_final = calculaMediaFinal(notas);
		}
		return media_final;
	}
	
	public void getAverageDisciplines(String forma_saida) {
		List<String> listAlunos = getAlunos(forma_saida);
		List<String> listDisciplinas = getDisciplinesDistintics(forma_saida);
		List<Double> listMedias = new ArrayList<Double>();
		
		BasicDBObject document = new BasicDBObject();
		DBCollection vectorCourses = null;
		
		System.out.println("lista de alunos");
		System.out.println(listAlunos.size());
		System.out.println("lista de disciplinas");
		System.out.println(listDisciplinas.size());
		
		try {
			vectorCourses = MongoConnection.getInstance().getDB().getCollection(
			"vector_courses_training_set2"+forma_saida.toLowerCase().trim().replace(" ", "").replace("(crit.01)", "").replace("(crit.02)", "02")+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (int i = 0; i < listAlunos.size(); i++) {
			listMedias = new ArrayList<Double>();
			for (int j = 0; j < listDisciplinas.size(); j++) {
				listMedias.add(getAverage(forma_saida, listAlunos.get(i), listDisciplinas.get(j)));
			}
			
			System.out.println("id_aluno");
			System.out.println(listAlunos.get(i));
			System.out.println("list medias");
			System.out.println(listMedias);
			System.out.println(" ");
			
			document = new BasicDBObject();
			document.put("id_aluno", listAlunos.get(i));
			document.put("vector_courses", listMedias);
			
//			vectorCourses.save(document);

		}
	}
	
	public List<Double> getVectorCourses(String forma_saida, String id_aluno){
		DBCollection vectorCourses = null;

		try {
			vectorCourses = MongoConnection.getInstance().getDB().getCollection(
			"vector_courses_training_set2"+forma_saida.toLowerCase().trim().replace(" ", "").replace("(crit.01)", "").replace("(crit.02)", "02")+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		DBObject match = new BasicDBObject("$match", new BasicDBObject("id_aluno", id_aluno));
		
		DBObject id = new BasicDBObject();
		id.put("id_aluno", "$id_aluno");
		id.put("vector_courses", "$vector_courses");
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
					
		List<DBObject> pipeline = Arrays.asList(match, group);
		AggregationOutput output = vectorCourses.aggregate(pipeline);
										
		List<Double> result = new ArrayList<Double>();
		
		for (DBObject dbo : output.results()) {
//			System.out.println("dbo: " +dbo);
			DBObject getId = (DBObject) dbo.get("_id");
			String temp = getId.get("vector_courses").toString();
			temp = temp.replace("[", "").replace("]", "").replace(" ", "");
			String [] notas = temp.split(",");
			for (int i = 0; i < notas.length; i++) {
				result.add(Double.parseDouble(notas[i]));
			}
		}
		
		return result;
	}
	
	public String getRepresentante(String forma_saida) {
		List<String> listAlunos = getAlunos(forma_saida);
		
		List<List<String>> list = new ArrayList<List<String>>();
//		System.out.println("lista de alunos");
//		System.out.println(listAlunos.size());

		double menorDistancia = 2;
		String id_aluno_menor_distancia = "";
		
		for (int i = 0; i < listAlunos.size(); i++) {
			double total_media = 0.0;
			List<String> listInside = new ArrayList<String>();
			for (int j = 0; j < listAlunos.size(); j++) {
				List<Double> vectorCoursesAlunoA = getVectorCourses(forma_saida, listAlunos.get(i));
				List<Double> vectorCoursesAlunoB = getVectorCourses(forma_saida, listAlunos.get(j));
				
//				System.out.println("vectorCoursesAlunoA");
//				System.out.println();
				
				double media = 0.0;
				for (int k = 0; k < vectorCoursesAlunoA.size(); k++) {
					double notaA = vectorCoursesAlunoA.get(k);
					double notaB = vectorCoursesAlunoB.get(k);
					double nota_final = 1 - ((Math.abs(notaA - notaB))/10);
					media = media + nota_final;
				}
				media = media/vectorCoursesAlunoA.size();
				total_media = total_media + media;
			}
			total_media = total_media / listAlunos.size();
			listInside.add(listAlunos.get(i).toString());
			listInside.add(String.valueOf(total_media));
			
			if (total_media < menorDistancia) {
				menorDistancia = total_media;
				id_aluno_menor_distancia = listAlunos.get(i).toString();
			}
			list.add(listInside);
		}
		id_aluno_representante = id_aluno_menor_distancia;
		System.out.println("aluno representante");
		System.out.println(id_aluno_representante);
		System.out.println("menor distancia");
		System.out.println(menorDistancia);
		System.out.println("list");
		System.out.println(list);
		return id_aluno_representante;
	}
	
	public List<String> getAlunosGeral(String forma_saida){
		DBObject id = new BasicDBObject();
		id.put("id_aluno", "$id_aluno");
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
					
		List<DBObject> pipeline = Arrays.asList(group);
		AggregationOutput output = alunos.aggregate(pipeline);
								
		List<String> list = new ArrayList<String>();
		
		List<String> listAlunos = getAlunos(forma_saida);
//		System.out.println("list Alunosss");
//		System.out.println(listAlunos.size());
		int cont = 1;
		for (DBObject dbo : output.results()) {
//			System.out.println("dbo: " + dbo);
			DBObject getId = (DBObject) dbo.get("_id");
			String id_aluno = getId.get("id_aluno").toString();
			if (listAlunos.contains(id_aluno) == false) {
				list.add(id_aluno);
			}
			cont++;
		}
//		System.out.println("cont");
		System.out.println(cont);
		return list;
	}
	
	public double getAverageGeral(String id_aluno, String cod_ativ_curricular) {
		DBObject matchAluno = new BasicDBObject("$match", new BasicDBObject("id_aluno", id_aluno));
		DBObject matchDisciplina = new BasicDBObject("$match", new BasicDBObject("cod_ativ_curricular", cod_ativ_curricular));

		DBObject id = new BasicDBObject();
		id.put("id_aluno", "$id_aluno");
		id.put("cod_ativ_curricular", "$cod_ativ_curricular");
		id.put("media_final", "$media_final");
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
					
		List<DBObject> pipeline = Arrays.asList(matchAluno, matchDisciplina, group);
		AggregationOutput output = alunos.aggregate(pipeline);
								
		double media_final = 0.0;
		
		List<Double> notas = new ArrayList<Double>();
		
		for (DBObject dbo : output.results()) {
//			System.out.println("dbo: " +dbo);
			DBObject getId = (DBObject) dbo.get("_id");
			if (getId.get("media_final") != null) {
				media_final = Double.parseDouble(getId.get("media_final").toString());
			}
			notas.add(media_final);
		}
		
		if (notas.size() > 0) {
			media_final = calculaMediaFinal(notas);
		}
		return media_final;
	}
	
	public void getAverageDisciplinesGeral(String forma_saida) {
		List<String> listAlunos = getAlunosGeral(forma_saida);
		List<String> listDisciplinas = getDisciplinesDistintics(forma_saida);
//		System.out.println("lista de alunos geral");
//		System.out.println(listAlunos.size());
//		System.out.println("discplinas");
//		System.out.println(listDisciplinas.size());
		List<Double> listMedias = new ArrayList<Double>();
		
		BasicDBObject document = new BasicDBObject();
		DBCollection vectorCourses = null;
		
		try {
			vectorCourses = MongoConnection.getInstance().getDB().getCollection(
			"vector_courses_geral2"+forma_saida.toLowerCase().trim().replace(" ", "").replace("(crit.01)", "").replace("(crit.02)", "02")+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (int i = 0; i < listAlunos.size(); i++) {
			listMedias = new ArrayList<Double>();
			for (int j = 0; j < listDisciplinas.size(); j++) {
				listMedias.add(getAverageGeral(listAlunos.get(i), listDisciplinas.get(j)));
			}
			
//			System.out.println("id_aluno");
//			System.out.println(listAlunos.get(i));
//			System.out.println("list medias");
//			System.out.println(listMedias);
//			System.out.println(" ");
			System.out.println(i);
			
			document = new BasicDBObject();
			document.put("id_aluno", listAlunos.get(i));
			document.put("vector_courses", listMedias);
			
//			vectorCourses.save(document);

		}
	}
	
	public List<Double> getVectorCoursesGeral(String forma_saida, String id_aluno){
		DBCollection vectorCourses = null;

		try {
			vectorCourses = MongoConnection.getInstance().getDB().getCollection(
			"vector_courses_geral2"+forma_saida.toLowerCase().trim().replace(" ", "").replace("(crit.01)", "").replace("(crit.02)", "02")+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		DBObject match = new BasicDBObject("$match", new BasicDBObject("id_aluno", id_aluno));
		
		DBObject id = new BasicDBObject();
		id.put("id_aluno", "$id_aluno");
		id.put("vector_courses", "$vector_courses");
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
					
		List<DBObject> pipeline = Arrays.asList(match, group);
		AggregationOutput output = vectorCourses.aggregate(pipeline);
										
		List<Double> result = new ArrayList<Double>();
		
		for (DBObject dbo : output.results()) {
//			System.out.println("dbo: " +dbo);
			DBObject getId = (DBObject) dbo.get("_id");
			String temp = getId.get("vector_courses").toString();
			temp = temp.replace("[", "").replace("]", "").replace(" ", "");
			String [] notas = temp.split(",");
			for (int i = 0; i < notas.length; i++) {
				result.add(Double.parseDouble(notas[i]));
			}
		}
		
		return result;
	}
	
	public List<List<String>> getResponse(String forma_saida) {
		FileWriter writer;
		List<List<String>> listDistance = new ArrayList<List<String>>();

		try{
			writer = new FileWriter("ResponseGeral.txt");
			List<String> listAlunos = getAlunosGeral(forma_saida);
			
			
			for (int i = 0; i < listAlunos.size(); i++) {
					List<String> listInfo = new ArrayList<String>();
					List<Double> vectorCoursesAlunoRepresentante = getVectorCourses(forma_saida, id_aluno_representante);
					List<Double> vectorCoursesAluno = getVectorCoursesGeral(forma_saida, listAlunos.get(i));
					
					double media = 0.0;
					for (int k = 0; k < vectorCoursesAlunoRepresentante.size(); k++) {
						double notaA = vectorCoursesAlunoRepresentante.get(k);
						double notaB = vectorCoursesAluno.get(k);
						double nota_final = 1 - ((Math.abs(notaA - notaB))/10);
						media = media + nota_final;
					}
					media = media/vectorCoursesAlunoRepresentante.size();
						listInfo.add(listAlunos.get(i).toString());
						listInfo.add(String.valueOf(media));
						
						listDistance.add(listInfo);
						String dados = getDetailsAlunoGeral(listAlunos.get(i).toString(), forma_saida);
						writer.write("Aluno: " + listInfo + "\n");
						writer.write(dados+"\n\n");
			}
			writer.close();
		} catch(IOException ex){
			ex.printStackTrace();
		}
		
		return listDistance;
	}
	
	public String getDetailsAlunoGeral(String id_aluno, String forma_saida){
		DBObject match = new BasicDBObject("$match", new BasicDBObject("id_aluno", id_aluno));
		
		DBObject id = new BasicDBObject();
		id.put("id_aluno", "$id_aluno");
		id.put("nome_curso", "$nome_curso");
		id.put("cod_curso", "$cod_curso");
		id.put("versao_curso", "$versao_curso");
		id.put("cod_ativ_curricular", "$cod_ativ_curricular");
		id.put("nome_ativ_curricular", "$nome_ativ_curricular");
		id.put("media_final", "$media_final");
		id.put("descricao_situacao", "$descricao_situacao");
		id.put("ano", "$ano");
		id.put("periodo", "$periodo");
		id.put("creditos", "$creditos");
		id.put("carga_horaria_teorica", "$carga_horaria_teorica");
		id.put("carga_horaria_pratica", "$carga_horaria_pratica");
		id.put("forma_ingresso", "$forma_ingresso");
		id.put("ano_ingresso", "$ano_ingresso");
		id.put("forma_saida", "$forma_saida");
		id.put("ano_saida", "$ano_saida");
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
		
		DBObject sortFields = new BasicDBObject();
		sortFields.put("_id.ano", 1);
		DBObject sort = new BasicDBObject("$sort", sortFields);
					
		List<DBObject> pipeline = Arrays.asList(match, group, sort);
		AggregationOutput output = alunos.aggregate(pipeline);
										
		String info = "";
		
		List<String> listDisciplinas = getDisciplinesDistintics(forma_saida);
		
		for (DBObject dbo : output.results()) {
//			System.out.println("dbo: " +dbo);
			DBObject getId = (DBObject) dbo.get("_id");
			String cod_ativ_curricular = getId.get("cod_ativ_curricular").toString();
//			if (listDisciplinas.contains(cod_ativ_curricular)) {
				info = info + dbo + "\n\n";
//			}
		}
		
		return info;
	}
	
	public double getDistanciaMax(String forma_saida, String id_representante) {
		List<List<String>> listDistance = new ArrayList<List<String>>();
			List<String> listAlunos = getAlunos(forma_saida);
			
			double menorDistancia = 2;
			String id_aluno_menor_distancia = "";
			
			for (int i = 0; i < listAlunos.size(); i++) {
					List<String> listInfo = new ArrayList<String>();
					List<Double> vectorCoursesAlunoRepresentante = getVectorCourses(forma_saida, id_representante);
					List<Double> vectorCoursesAluno = getVectorCourses(forma_saida, listAlunos.get(i));
					
					double media = 0.0;
					for (int k = 0; k < vectorCoursesAlunoRepresentante.size(); k++) {
						double notaA = vectorCoursesAlunoRepresentante.get(k);
						double notaB = vectorCoursesAluno.get(k);
						double nota_final = 1 - ((Math.abs(notaA - notaB))/10);
						media = media + nota_final;
					}
					media = media/vectorCoursesAlunoRepresentante.size();
						listInfo.add(listAlunos.get(i).toString());
						listInfo.add(String.valueOf(media));
						
						listDistance.add(listInfo);
						
						if (media < menorDistancia) {
							menorDistancia = media;
							id_aluno_menor_distancia = listAlunos.get(i).toString();
						}
			}
		System.out.println(" ");
		System.out.println("aluno");
		System.out.println(id_aluno_menor_distancia);
		System.out.println("dist max");
		System.out.println(menorDistancia);
		return menorDistancia;
	}
	
	public double getDistanciaMediana(String forma_saida, String id_representante) {
		List<List<String>> listDistance = new ArrayList<List<String>>();
		List<String> listAlunos = getAlunos(forma_saida);
		List<Double> listAllDistance = new ArrayList<Double>();

			
			double medianaDistancia;
			
			for (int i = 0; i < listAlunos.size(); i++) {
					List<String> listInfo = new ArrayList<String>();
					List<Double> vectorCoursesAlunoRepresentante = getVectorCourses(forma_saida, id_representante);
					List<Double> vectorCoursesAluno = getVectorCourses(forma_saida, listAlunos.get(i));
					
					double media = 0.0;
					for (int k = 0; k < vectorCoursesAlunoRepresentante.size(); k++) {
						double notaA = vectorCoursesAlunoRepresentante.get(k);
						double notaB = vectorCoursesAluno.get(k);
						double nota_final = 1 - ((Math.abs(notaA - notaB))/10);
						media = media + nota_final;
					}
					media = media/vectorCoursesAlunoRepresentante.size();
					listAllDistance.add(media);
					listInfo.add(listAlunos.get(i).toString());
					listInfo.add(String.valueOf(media));
						
					listDistance.add(listInfo);
						
//						if (media < menorDistancia) {
//							menorDistancia = media;
//							id_aluno_menor_distancia = listAlunos.get(i).toString();
//						}
			}
		
		Collections.sort(listAllDistance);
		System.out.println("lista de distancias");
		System.out.println(listAllDistance);
		
		medianaDistancia = listAllDistance.get(listAllDistance.size()/2);
		
		System.out.println(" ");
		System.out.println("distancia mediana ");
		System.out.println(medianaDistancia);
		
		return medianaDistancia;
	}
	
	public List<List<String>> getResultadosExperimentosDistanceDoPerfil(String forma_saida, String id_representante, double distMax) {
		List<List<String>> listDistancesAprovados = new ArrayList<List<String>>();
		List<List<String>> listDistancesReprovados = new ArrayList<List<String>>();
		List<String> listAlunos = list_excluidos_perfil;
		
//		System.out.println("list excl size");
//		System.out.println(list_excluidos_perfil.size());
			
			for (int i = 0; i < listAlunos.size(); i++) {
					List<String> listInfo = new ArrayList<String>();
					List<Double> vectorCoursesAlunoRepresentante = getVectorCourses(forma_saida, id_representante);
					List<Double> vectorCoursesAluno = getVectorCoursesGeral(forma_saida, listAlunos.get(i));
					
					double media = 0.0;
					for (int k = 0; k < vectorCoursesAlunoRepresentante.size(); k++) {
						double notaA = vectorCoursesAlunoRepresentante.get(k);
						double notaB = vectorCoursesAluno.get(k);
						double nota_final = 1 - ((Math.abs(notaA - notaB))/10);
						media = media + nota_final;
					}
					media = media/vectorCoursesAlunoRepresentante.size();
					listInfo.add(listAlunos.get(i).toString());
					listInfo.add(String.valueOf(media));
						if (media >= distMax) {
							listDistancesAprovados.add(listInfo);
						} else {
							listDistancesReprovados.add(listInfo);
						}
			}
		System.out.println("tamanho da lista dos alunos excluídos do conjunto e classificados como pertences ao perfil");
		System.out.println(listDistancesAprovados.size());
		System.out.println("tamanho da lista dos alunos excluídos do conjunto e classificados como não pertences ao perfil");
		System.out.println(listDistancesReprovados.size());
		System.out.println(" ");
		return listDistancesAprovados;
	}
	
	public List<List<String>> getResultadosExperimentosDistanceGeral(String forma_saida, String id_representante, double distMax) {
		List<List<String>> listDistancesAprovados = new ArrayList<List<String>>();
		List<List<String>> listDistancesReprovados = new ArrayList<List<String>>();
		List<String> listAlunos = getAlunosGeral(forma_saida);
//			System.out.println("vetor geral do representante");
//			System.out.println(getVectorCourses(forma_saida, id_representante));
//			
			for (int i = 0; i < listAlunos.size(); i++) {
					List<String> listInfo = new ArrayList<String>();
					List<Double> vectorCoursesAlunoRepresentante = getVectorCourses(forma_saida, id_representante);
					List<Double> vectorCoursesAluno = getVectorCoursesGeral(forma_saida, listAlunos.get(i));
//					System.out.println(" ");
//					System.out.println("vetor do aluno");
//					System.out.println(vectorCoursesAluno);
					double media = 0.0;
					for (int k = 0; k < vectorCoursesAlunoRepresentante.size(); k++) {
						double notaA = vectorCoursesAlunoRepresentante.get(k);
						double notaB = vectorCoursesAluno.get(k);
						double nota_final = 1 - ((Math.abs(notaA - notaB))/10);
						media = media + nota_final;
					}
					media = media/vectorCoursesAlunoRepresentante.size();
					listInfo.add(listAlunos.get(i).toString());
					listInfo.add(String.valueOf(media));
						if (media >= distMax) {
							listDistancesAprovados.add(listInfo);
						} else {
//							System.out.println(" ");
//							System.out.println("vetor do aluno");
//							System.out.println(vectorCoursesAluno);
							listDistancesReprovados.add(listInfo);
						}
						
			}
		System.out.println("tamanho da lista dos alunos em geral classificados como pertences ao perfil");
		System.out.println(listDistancesAprovados.size());
		System.out.println(" ");
		System.out.println("tamanho da lista dos alunos em geral classificados como não pertences ao perfil");
		System.out.println(listDistancesReprovados.size());
		System.out.println(" ");
		return listDistancesAprovados;
	}
	
	public static void main(String args[]) {
		AnaliseDeFormado ativ= new AnaliseDeFormado("ciencia da computacao");
		
		// FORMA O CONJUNTO DE ALUNOS QUE ESTARÃO NO CONJUNTO DO PERFIL SELECIONADO DE ACORDO COM A PORCENTAGEM REQUERIDA
		ativ.filterByFormado("formado", 0.75);
		
//		System.out.println(ativ.getAlunos("formado").size());
//		System.out.println(ativ.getDisciplinesDistintics("formado").size());
		
		// CALCULA A MEDIA DE NOTAS DOS ALUNOS DO CONJUNTO EM CADA DISCIPLINA DO VETOR DE DISCPLINAS QUE FORAM 
		// SELECIONADAS DENTRO DO CONJUNTO DE DISCIPLINAS FEITAS PELOS ALUNOS DENTRO DO CONJUNTO
		ativ.getAverageDisciplines("formado");
		
		// ENCONTRA O REPRESENTANTE DO CONJUNTO FORMADO DENTRO DAQUELE TIPO DE EVASAO REQUERIDO, ATRAVÉS
		// DE UM CALCULO DE MÉDIA DAS DISTÂNCIAS DE CADA ALUNO DENTRO DO CONJUNTO PARA O RESTANTE DOS ALUNOS DO CONJUNTO
//		String representante = ativ.getRepresentante("formado");
		
		// CALCULA A MEDIA DE NOTAS DOS ALUNOS ("RESTO DO MUNDO") EM CADA DISCIPLINA DO VETOR DE DISCPLINAS QUE FORAM 
		// SELECIONADAS DENTRO DO CONJUNTO DE DISCIPLINAS FEITAS PELOS ALUNOS DENTRO DO CONJUNTO
//		ativ.getAverageDisciplinesGeral("formado");
		
		// CALCULA A DISTANCIA MAXIMA DENTRO DO CONJUNTO, ATRAVÉS DE UM CALCULO PARA DESCOBRIR QUAL O VALOR DA DISTANCIA
		// DO ALUNO MAIS DISTANTE DO REPRESENTANTE DE DENTRO DO CONJUNTO
//		double distMax = ativ.getDistanciaMax("formado", representante);
//		double distMax = ativ.getDistanciaMediana("formado", representante);
		
		// VERIFICA QUANTIDADE DE ALUNOS DE DENTRO DO PERFIL PORÉM QUE NÃO ENTRARAM NO CONJUNTO E QUE FORAM CLASSIFICADOS
		// PELO ALGORITMO COMO PERTENCENTES AO PERFIL 
//		ativ.getResultadosExperimentosDistanceDoPerfil("formado", representante, distMax);
		
		// VERIFICA QUANTIDADE DE ALUNOS FORA DO PERFIL (RESTO DO MUNDO) QUE FORAM CLASSIFICADOS
		// PELO ALGORITMO COMO PERTENCENTES AO PERFIL
//		ativ.getResultadosExperimentosDistanceGeral("formado", representante, distMax);
				
//		System.out.println(ativ.getResponse("jubilado (crit. 01)"));
//		System.out.println();
	}
}
